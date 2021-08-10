package com.tozny.e3db

import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tozny.e3db.Client.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.net.URI
import java.util.*
import kotlin.collections.HashMap

class Realm @JvmOverloads constructor(realmName: String?, appName: String?, brokerTargetURL: URI?, apiURL: URI? = URI("https://api.e3db.com"), certificatePinner: CertificatePinner? = null) {
  companion object {
    @JvmStatic
    val crypto = Platform.crypto!!

    @JvmStatic
    private val backgroundExecutor = Client.backgroundExecutor

    @JvmStatic
    private val uiExecutor = Client.uiExecutor

    internal val mapper = ObjectMapper().registerKotlinModule().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)

    internal fun deriveNoteCreds(realmName: String, userName: String, password: String, noteType: CredentialType): DerivedNoteCreds {
      val nameSeed = "${userName.toLowerCase(Locale.US)}@realm:$realmName".let { seed ->
        when (noteType) {
          CredentialType.EMAIL_OTP -> "broker:$seed"
          CredentialType.TOZNY_OTP -> "tozny_otp:$seed"
          CredentialType.PASSWORD -> seed
        }
      }
      val noteName = crypto.hashString(nameSeed)
      val deriveEncryptionKeypair = crypto.deriveEncryptionKeypair(password.toCharArray(), nameSeed.toByteArray())
      val publicEncryption = Base64.encodeURL(deriveEncryptionKeypair?.publicKey)
      val secretEncryption = Base64.encodeURL(deriveEncryptionKeypair?.privateKey)
      val deriveSigningKeyPair = crypto.deriveSigningKeyPair(password.toCharArray(), "$publicEncryption$secretEncryption".toByteArray())
      return DerivedNoteCreds(Base64.encodeURL(noteName), deriveEncryptionKeypair, deriveSigningKeyPair)
    }
  }

  private val identityClient: IdentityServiceClient
  private lateinit var defaultBrokerClient: BrokerClient
  private val realmName: String
  private val appName: String
  private val brokerTargetURL: URI
  private val apiURL: URI
  private val certificatePinner: CertificatePinner?
  private lateinit var realmInfo: PublicRealmInfo

  init {
    require(!realmName.isNullOrEmpty()) { illegalArgumentEmpty("realmName") }
    this.realmName = realmName
    require(!appName.isNullOrEmpty()) { illegalArgumentEmpty("appName") }
    this.appName = appName
    require(brokerTargetURL != null) { illegalArgumentNull("brokerTargetURL", "URI") }
    this.brokerTargetURL = brokerTargetURL
    require(apiURL != null) { illegalArgumentNull("apiURL", "URI") }
    this.apiURL = apiURL
    this.certificatePinner = certificatePinner
    val anonymousClient = OkHttpClient().newBuilder()
        .apply {
          certificatePinner?.let { pinner ->
            certificatePinner(pinner)
          }
        }.build()
    val retrofitBuilder = Retrofit
        .Builder().apply {
          client(anonymousClient)
          callbackExecutor(uiExecutor)
          baseUrl(apiURL.toURL())
          addConverterFactory(
              JacksonConverterFactory.create(mapper)
          )
        }

    identityClient = retrofitBuilder.build().create()

    // TODO: All network calls are done on background threads (backgroundExecutor.execute), can this run in init?
    // realmName get from call to getPublicRealm endpoint
    // Most cases of this.realmName should be this.domainName (reference js-sdk for which needs to change), keycloak expects this
    backgroundExecutor.execute {
      try {
        val publicRealmInfo = identityClient.getPublicRealmInfo(realmName).execute()
        when {
          publicRealmInfo.isSuccessful -> {
            publicRealmInfo.body().also {
              if (it != null) {
                this.realmInfo = it
                retrofitBuilder.baseUrl("${apiURL}/v1/identity/broker/realm/${this.realmInfo.domain}/") // this.realmName --> this.domainName which is new instance variable for realms
                defaultBrokerClient = retrofitBuilder.build().create()
              }
            }
          }
        }
      } catch (e: Exception) {
        null // TODO: How to throw proper exception without a resultHandler in init?
      }
    }
  }

  private fun createTSV1Client(privateSigningKey: ByteArray, publicSigningKey: ByteArray = crypto.getPublicSigningKey(privateSigningKey), clientID: UUID? = null, additionalHeaders: Map<String, String> = mapOf()): OkHttpClient {
    val tsv1HttpClient = OkHttpClient().newBuilder()
        .apply {
          certificatePinner?.let { pinner ->
            certificatePinner(pinner)
          }
          clientID?.let {
            addInterceptor(TSV1Interceptor(privateSigningKey, publicSigningKey, it.toString(), additionalHeaders))
          }
              ?: run { addInterceptor(TSV1Interceptor(privateSigningKey, publicSigningKey, "", additionalHeaders)) }
        }.build()
    return tsv1HttpClient
  }

  private fun createTSV1IdentityClient(privateSigningKey: ByteArray, publicSigningKey: ByteArray = crypto.getPublicSigningKey(privateSigningKey), clientID: UUID? = null, additionalHeaders: Map<String, String> = mapOf()): IdentityServiceClient {
    val tsv1HttpClient = OkHttpClient().newBuilder()
        .apply {
          certificatePinner?.let { pinner ->
            certificatePinner(pinner)
          }
          clientID?.let {
            addInterceptor(TSV1Interceptor(privateSigningKey, publicSigningKey, it.toString(), additionalHeaders))
          }
              ?: run { addInterceptor(TSV1Interceptor(privateSigningKey, publicSigningKey, "", additionalHeaders)) }
        }.build()
    return Retrofit
        .Builder().apply {
          client(tsv1HttpClient)
          callbackExecutor(uiExecutor)
          baseUrl(apiURL.toURL())
          addConverterFactory(
              JacksonConverterFactory.create(mapper)
          )
        }.build().create()
  }


  @JvmOverloads
  @Throws(E3DBCryptoException::class)
  fun register(userName: String?, password: String?, token: String?, email: String?, firstName: String?, lastName: String?, emailEACPExpiryMinutes: Int = 60, resultHandler: ResultHandler<PartialIdentityClient>?, certificatePinner: CertificatePinner? = null) {
    require(!userName.isNullOrBlank()) { illegalArgumentBlank("userName") }
    require(!password.isNullOrEmpty()) { illegalArgumentEmpty("password") }
    require(!token.isNullOrBlank()) { illegalArgumentBlank("token") }
    require(!email.isNullOrBlank()) { illegalArgumentBlank("email") }
    require(resultHandler != null) { illegalArgumentNull("resultHandler", "ResultHandler") }

    backgroundExecutor.execute {
      try {
        val username = userName.toLowerCase(Locale.US)
        val newSigningKey = generateSigningKey()
        val publicSigningKey = getPublicSigningKey(newSigningKey)
        val newEncryptionKey = generateKey()
        val publicEncryptionKey = getPublicKey(newEncryptionKey)
        val response = identityClient.registerIdentity(
                RegisterIdentityRequest(
                    token,
                    realmName,
                    IdentityInfo(realmName, username, Curve25519PublicKey(publicEncryptionKey), Ed25519PublicKey(publicSigningKey), firstName, lastName, null, null, null, null)))
            .execute()
        when {
          response.isSuccessful -> {
            response.body()?.let { body ->
              //Instantiate client here
              val storageClientConfig = Config(body.identityInfo.apiKeyID, body.identityInfo.apiKeySecret, body.identityInfo.toznyClientID, null, apiURL.toString(), newEncryptionKey, newSigningKey)
              val client = ClientBuilder().apply {
                fromConfig(storageClientConfig)
                certificatePinner?.let { pinner -> setCertificatePinner(pinner) }
              }.build()
              val deriveNoteCreds = deriveNoteCreds(realmName, username, password, CredentialType.PASSWORD)
              val storageConfig = storageClientConfig.json()
              val identityConfig = IdentityConfig(apiURL, appName, brokerTargetURL.toString(), realmName, body.identityInfo.userId, username, null)
              val identityConfigAsString = mapper.writeValueAsString(identityConfig)
              val data = mapOf("config" to identityConfigAsString, "storage" to storageConfig)
              val noteOptions = NoteOptions().apply {
                noteName = deriveNoteCreds.noteName
                maxViews = -1
                expires = false
                eacp = EACP.Builder().tozIDEACP(TozIDEACP(realmName)).build()
              }
              val idClientData = RecordData(data)
              client.writeNote(
                  idClientData,
                  deriveNoteCreds.encryptionKeys.publicKey,
                  deriveNoteCreds.signingKeys.publicKey,
                  noteOptions
              ) { noteResult ->
                noteResult!!.let {
                  when {
                    it.isError -> uiExecutor.execute { resultHandler.handle(ErrorResult(it.asError().error())) }
                    else -> body.realmBrokerIdentityToznyId?.let { brokerID ->
                      try {
                        backgroundExecutor.execute {
                          // Write the broker email reset notes
                          client.getClientInfo(brokerID)?.let { clientInfo ->
                            listOf(firstName, lastName).filterNot(String?::isNullOrBlank).joinToString(" ").ifEmpty { null }
                            val brokerKeyNoteName = Base64.encodeURL(crypto.hashString("brokerKey:$username@realm:$realmName"))
                            val brokerKey = Base64.encodeURL(crypto.randomBytes(64))
                            val brokerNoteCreds = deriveNoteCreds(realmName, username, brokerKey, CredentialType.EMAIL_OTP)
                            val brokerNoteOptions = NoteOptions().apply {
                              noteName = brokerKeyNoteName
                              maxViews = -1
                              expires = false
                              eacp = EACP.Builder().emailEACP(EmailEACP(
                                      email,
                                      "claim_account",
                                      brokerTargetURL.toString(),
                                      listOf(firstName, lastName).filterNot(String?::isNullOrBlank).joinToString(" ").ifBlank { null }?.let { identityName -> mapOf("name" to identityName) }
                                              ?: run { HashMap<String, String>() },
                                      emailEACPExpiryMinutes
                              )).build()
                            }
                            client.writeNote(
                                    RecordData(mapOf("brokerKey" to brokerKey, "username" to username)),
                                    clientInfo.encryptionKey!!,
                                    clientInfo.signingKey!!,
                                    brokerNoteOptions
                            ) { brokerKeyNoteResultOpt ->
                              brokerKeyNoteResultOpt!!.let { brokerKeyNoteResult ->
                                when {
                                  brokerKeyNoteResult.isError -> uiExecutor.execute { resultHandler.handle(ErrorResult(it.asError().other())) }
                                  else -> {
                                    val brokerIdCredsNoteOptions = NoteOptions().apply {
                                      noteName = brokerNoteCreds.noteName
                                      maxViews = -1
                                      expires = false
                                      eacp = EACP.Builder().lastAccessEACP(LastAccessEACP(brokerKeyNoteResult.asValue().noteID)).build()
                                    }
                                    client.writeNote(
                                            idClientData,
                                            brokerNoteCreds.encryptionKeys.publicKey,
                                            brokerNoteCreds.signingKeys.publicKey,
                                            brokerIdCredsNoteOptions
                                    ) { brokerIdCredsNoteOpt ->
                                      brokerIdCredsNoteOpt!!.let { brokerIdCredsNoteResult ->
                                        when {
                                          brokerIdCredsNoteResult.isError -> resultHandler.handle(ErrorResult(it.asError().other()))
                                          else -> {
                                            // Write the broker otp reset notes
                                            val brokerOTPNoteName = Base64.encodeURL(crypto.hashString("broker_otp:$username@realm:$realmName"))
                                            val brokerOTPKey = Base64.encodeURL(crypto.randomBytes(64))
                                            val brokerOTPNoteCreds = deriveNoteCreds(realmName, username, brokerOTPKey, CredentialType.TOZNY_OTP)
                                            val brokerOTPNoteOptions = NoteOptions().apply {
                                              noteName = brokerOTPNoteName
                                              maxViews = -1
                                              expires = false
                                              eacp = EACP.Builder().toznyOTPEACP(ToznyOTPEACP(true)).build()
                                            }
                                            client.writeNote(
                                                    RecordData(mapOf("brokerKey" to brokerOTPKey, "username" to username)),
                                                    clientInfo.encryptionKey!!,
                                                    clientInfo.signingKey!!,
                                                    brokerOTPNoteOptions
                                            ) { brokerOTPNoteOpt ->
                                              brokerOTPNoteOpt!!.let { brokerOTPNoteResult ->
                                                when {
                                                  brokerOTPNoteResult.isError -> uiExecutor.execute { resultHandler.handle(ErrorResult(it.asError().other())) }
                                                  else -> {
                                                    val brokerIDCredsNoteOptions = NoteOptions().apply {
                                                      noteName = brokerOTPNoteCreds.noteName
                                                      maxViews = -1
                                                      expires = false
                                                      eacp = EACP.Builder().lastAccessEACP(LastAccessEACP(brokerOTPNoteResult.asValue().noteID)).build()
                                                    }
                                                    client.writeNote(
                                                            idClientData,
                                                            brokerOTPNoteCreds.encryptionKeys.publicKey,
                                                            brokerOTPNoteCreds.signingKeys.publicKey,
                                                            brokerIDCredsNoteOptions
                                                    ) { brokerOTPCredsNoteOpt ->
                                                      brokerOTPCredsNoteOpt!!.let { brokerOTPCredsNote ->
                                                        when {
                                                          brokerOTPCredsNote.isError -> uiExecutor.execute { resultHandler.handle(ErrorResult(it.asError().other())) }
                                                          else -> uiExecutor.execute { resultHandler.handle(ValueResult(PartialIdentityClient(client, identityConfig))) }
                                                        }
                                                      }
                                                    }
                                                  }
                                                }
                                              }
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                        } catch (e: E3DBException) {
                          uiExecutor.execute { resultHandler.handle(ErrorResult(e)) }
                        }
                    } ?: run {
                      // If there is no broker do not write broker notes
                      uiExecutor.execute { resultHandler.handle(ValueResult(PartialIdentityClient(client, identityConfig))) }
                    }
                  }
                }
              }
            } ?: run {
              (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(response.code(), "${response.message()}. No body response body found on successful request"))) })
            }
          }
          else -> {
            (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(response.code(), response.message()))) })
          }
        }
      } catch (e: Exception) {
        (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during user register", e))) })
      }
    }
  }

  @JvmOverloads
  @Throws(E3DBCryptoException::class, E3DBException::class)
  fun login(userName: String?, password: String?, credentialType: CredentialType? = CredentialType.PASSWORD, actionHandler: LoginActionHandler?, resultHandler: ResultHandler<IdentityClient>?) {
    require(!userName.isNullOrBlank()) { illegalArgumentBlank("userName") }
    require(!password.isNullOrEmpty()) { illegalArgumentEmpty("password") }
    require(credentialType != null) { illegalArgumentNull("credentialType", "CredentialType") }
    require(resultHandler != null) { illegalArgumentNull("resultHandler", "ResultHandler") }
    backgroundExecutor.execute {
      try {
        val username = userName.toLowerCase(Locale.US)
        val deriveNoteCreds = deriveNoteCreds(realmInfo.name, username, password, credentialType)
        val anonymousIdentityClient = createTSV1IdentityClient(deriveNoteCreds.signingKeys.privateKey, deriveNoteCreds.signingKeys.publicKey)
        val sessionStart = anonymousIdentityClient.loginIdentity(LoginRequest(username, realmInfo.name, appName)).execute()
        when {
          sessionStart.isSuccessful -> {
            val fieldMap = HashMap<String, String>()
            fieldMap.apply {
              sessionStart.body()?.fields()?.forEach {
                put(it.key, it.value.asText())
              }
            }
            val sessionRequest = identityClient.sessionRequest(realmInfo.name, fieldMap).execute()
            when {
              sessionRequest.isSuccessful -> {
                var body: CompleteLoginAction
                body = sessionRequest.body()!!
                var madeFinalRequest = false
                while (!madeFinalRequest) {
                  when (body.loginActionType) {
                    "fetch" -> {
                      madeFinalRequest = true
                      val finalRequest = anonymousIdentityClient.loginredirect(LoginRedirectRequest(
                          realmInfo.name,
                          body.context["session_code"] ?: "",
                          body.context["execution"] ?: "",
                          body.context["tab_id"] ?: "",
                          body.context["client_id"] ?: "",
                          body.context["auth_session_id"] ?: ""
                      )).execute()
                      when {
                        finalRequest.isSuccessful -> {
                          val finalRequestBody = finalRequest.body()!!
                          readAnonymousNote(
                              null,
                              deriveNoteCreds.noteName,
                              deriveNoteCreds.signingKeys.privateKey,
                              deriveNoteCreds.signingKeys.publicKey,
                              deriveNoteCreds.encryptionKeys.privateKey,
                              this.apiURL,
                              mapOf("X-TOZID-LOGIN-TOKEN" to finalRequestBody["access_token"]!!.asText()),
                              this.certificatePinner
                          ) { noteResponseOpt ->
                            noteResponseOpt!!.let { noteResponse ->
                              when {
                                noteResponse.isError -> {
                                  (uiExecutor.execute { resultHandler.handle(ErrorResult(noteResponse.asError().error())) })
                                }
                                else -> {
                                  val message = noteResponse.asValue()!!
                                  message.data["storage"]?.let { storageConfig ->
                                    message.data["config"]?.let { identityConfigJson ->
                                      val storageClient = ClientBuilder().fromConfig(Config.fromJson(storageConfig)).build()
                                      val identityConfig = IdentityConfig.fromJson(identityConfigJson)
                                      AgentToken.fromJson(finalRequestBody)?.let { agentToken ->
                                        uiExecutor.execute { resultHandler.handle(ValueResult(IdentityClient(storageClient, identityConfig, agentToken, certificatePinner))) }
                                      }
                                          ?: run {
                                            (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("Identity Token could not be parsed"))) })
                                          }
                                    }
                                  }
                                      ?: run { (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An error occurred while logging in, config is unparsable"))) }) }
                                }
                              }
                            }
                          }
                        }
                        else -> (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(finalRequest.code(), finalRequest.message()))) })
                      }
                    }
                    else -> {
                      actionHandler?.let {
                        val handleAction = it.handleAction(LoginAction(body))
                        val url = URI(body.actionURL)
                        val loginActionClient = createTSV1Client(
                            deriveNoteCreds.signingKeys.privateKey,
                            deriveNoteCreds.signingKeys.publicKey
                        )
                        val request = Request.Builder()
                        request.url(url.toURL())
                        when (body.contentType) {
                          "application/x-www-form-urlencoded" -> {
                            request.post(FormBody.Builder().apply {
                              handleAction.entries.forEach { actionEntry ->
                                addEncoded(actionEntry.key, actionEntry.value.toString())
                              }
                            }.build())

                          }
                          else -> {


                            request.post(mapper.writeValueAsString(handleAction).toRequestBody("application/json".toMediaType()))
                          }
                        }
                        val loginActionResponse = loginActionClient.newCall(request.build()).execute()
                        when (loginActionResponse.isSuccessful) {
                          false -> {
                            madeFinalRequest = true
                            throw E3DBException.find(loginActionResponse.code, loginActionResponse.message)
                          }
                          true -> {
                            val content = loginActionResponse.body?.string()
                            content?.let {
                              body = mapper?.readValue(content) ?: run {
                                madeFinalRequest = true
                                throw E3DBException("Login action response was not parsable login action failed")
                              }
                            } ?: run {
                              madeFinalRequest = true
                              throw E3DBException("Login action response was not parsable login action failed")
                            }
                          }
                        }

                      } ?: run {
                        madeFinalRequest = true
                        (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("During login an action needed to be handled but no LoginActionHandler was provided"))) })
                      }
                    }
                  }
                }
              }
              else -> (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(sessionRequest.code(), sessionRequest.message()))) })

            }
          }
          else -> {
            (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(sessionStart.code(), sessionStart.message()))) })
          }
        }
      } catch (e: Exception) {
        (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during user login", e))) })
      }
    }
  }

  @JvmOverloads
  fun initiateBrokerLogin(userName: String?, brokerClient: BrokerClient = this.defaultBrokerClient, resultHandler: ResultHandler<Void>?) {
    require(!userName.isNullOrBlank()) { illegalArgumentBlank("userName") }
    require(resultHandler != null) { illegalArgumentNull("resultHandler", "ResultHandler") }
    val username = userName.toLowerCase(Locale.US)
    backgroundExecutor.execute {
      try {
        val challengeResponse = brokerClient.brokerChallenge(InitiateChallengeRequest(username, "challenge")).execute()
        when {
          challengeResponse.isSuccessful -> {
            uiExecutor.execute { resultHandler.handle(ValueResult(null)) }
          }
          else -> {
            (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(challengeResponse.code(), challengeResponse.message()))) })
          }
        }
      } catch (e: Exception) {
        (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during initiateBrokerLogin", e))) })
      }
    }
  }


  @JvmOverloads
  fun completeBrokerLogin(authResponse: Map<String, Any>?, noteId: UUID?, brokerType: CredentialType?, brokerClient: BrokerClient = this.defaultBrokerClient, resultHandler: ResultHandler<PartialIdentityClient>?) {
    requireNotNull(authResponse) { illegalArgumentNull("authResponse", "Object") }
    requireNotNull(noteId) { illegalArgumentNull("noteId", "UUID") }
    require(brokerType != null) { illegalArgumentNull("brokerType", "CredentialType") }
    require(resultHandler != null) { illegalArgumentNull("resultHandler", "ResultHandler") }
    backgroundExecutor.execute {
      try {
        val privateKey = crypto.newPrivateKey()
        val privateSigningKey = crypto.newPrivateSigningKey()
        val publicSigningKey = crypto.getPublicSigningKey(privateSigningKey)
        val completeBrokerLoginRequest = CompleteBrokerLoginRequest(
            authResponse,
            noteId,
            Base64.encodeURL(crypto.getPublicKey(privateKey)),
            Base64.encodeURL(publicSigningKey)
        )
        val completeBrokerLogin = brokerClient.completeBrokerLogin(completeBrokerLoginRequest).execute()
        when {
          completeBrokerLogin.isSuccessful -> {
            completeBrokerLogin.body()!!.transferId.let { transferId ->
              readAnonymousNote(transferId, null, privateSigningKey, publicSigningKey, privateKey, this.apiURL, null, this.certificatePinner) { readNoteResponse ->
                when {
                  readNoteResponse.isError -> {
                    (uiExecutor.execute { resultHandler.handle(ErrorResult(readNoteResponse.asError().error())) })
                  }
                  else -> {
                    readNoteResponse.asValue().data?.let { noteData ->
                      noteData["brokerKey"]?.let { brokerKey ->
                        noteData["username"]?.let { username ->
                          deriveNoteCreds(this.realmName, username, brokerKey, brokerType).let { derivedCreds ->
                            readAnonymousNote(
                                null,
                                derivedCreds.noteName,
                                derivedCreds.signingKeys.privateKey,
                                derivedCreds.signingKeys.publicKey,
                                derivedCreds.encryptionKeys.privateKey,
                                this.apiURL,
                                null,
                                this.certificatePinner
                            ) { storedCredsResponse ->
                              when {
                                storedCredsResponse.isError -> (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("While completing broker login an error occurred", storedCredsResponse.asError().error()))) })
                                else -> {
                                  storedCredsResponse.asValue()?.data?.let { storedCreds ->
                                    storedCreds["storage"]?.let { storageConfig ->
                                      storedCreds["config"]?.let { identityConfigJson ->
                                        val storageClient = ClientBuilder().fromConfig(Config.fromJson(storageConfig)).build()
                                        val identityConfig = IdentityConfig.fromJson(identityConfigJson)
                                        uiExecutor.execute { resultHandler.handle(ValueResult(PartialIdentityClient(storageClient, identityConfig))) }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    } ?: run {
                      (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("While completing broker login the broker login data was corrupt"))) })
                    }
                  }
                }
              }
            }
          }
          else -> {
            (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(completeBrokerLogin.code(), completeBrokerLogin.message()))) })
          }
        }
      } catch (e: Exception) {
        (uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during CompleteBrokerLogin", e))) })
      }
    }
  }
}


enum class CredentialType {
  PASSWORD,
  EMAIL_OTP,
  TOZNY_OTP,
}

data class IdentityConfig(
    @JsonProperty("api_url") val apiURL: URI,
    @JsonProperty("app_name") val appName: String,
    @JsonProperty("broker_target_url") val brokerTargetUrl: String,
    @JsonProperty("realm_name") val realmName: String,
    @JsonProperty("user_id") val userId: Int?,
    @JsonProperty("realm_domain") val realmDomain: String?,

    // readAnonymousNote response did not contain a username field, so we make
    // it optional here. This caused replacePassword and validatePassword methods
    // in the Identity class to use a non-null assertion when constructing
    // IdentityConfig objects. 
    @JsonProperty("username") val username: String?
) {
  companion object {
    fun fromJson(json: String): IdentityConfig {
      val objectMapper = ObjectMapper()
      return objectMapper.readValue(json)
    }
  }
}


class DerivedNoteCreds(val noteName: String, val encryptionKeys: E3DBKeyPair, val signingKeys: E3DBKeyPair)

internal fun illegalArgumentEmpty(name: String): String {
  return "${name}: (string): was null or empty."
}

internal fun illegalArgumentBlank(name: String): String {
  return "${name}: (string): was null or blank."
}

internal fun illegalArgumentNull(name: String, type: String): String {
  return "${name}: ($type): may not be provided as null."
}

interface LoginActionHandler {
  fun handleAction(completeLoginAction: LoginAction): Map<String, Any>
}

data class LoginAction(val type: String, val fields: Map<String, FieldType>, val context: Map<String, String>, val errorMessage: LoginActionErrorMessage?) {
  constructor(completeLoginAction: CompleteLoginAction) : this(completeLoginAction.loginActionType, completeLoginAction.fields, completeLoginAction.context, completeLoginAction.errorMessage)
}