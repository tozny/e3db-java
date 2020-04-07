package com.tozny.e3db

import com.fasterxml.jackson.databind.JsonNode
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.create
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


open class PartialIdentityClient @JvmOverloads constructor(val client: Client, val identityConfig: IdentityConfig, certificatePinner: CertificatePinner? = null) {
  fun updatePassword(currentPassword: String, newPassword: String, resultHandler: ResultHandler<Void>) {
    Client.backgroundExecutor.run {
      try {
        when (validatePassword(currentPassword)) {
          false -> Client.uiExecutor.execute { resultHandler.handle(ErrorResult<Void>(E3DBException("Old password could not be validated"))) }
          true -> {
            replacePassword(newPassword, resultHandler)
          }
        }
      } catch (e: Exception) {
        (Client.uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during update password", e))) })
      }
    }
  }

  private fun validatePassword(currentPassword: String): Boolean {
    val deriveNoteCreds = Realm.deriveNoteCreds(identityConfig.realmName, identityConfig.username, currentPassword, CredentialType.PASSWORD)
    val anonymousNoteClient = Client.getAnonymousNoteClient(deriveNoteCreds.signingKeys.privateKey, deriveNoteCreds.signingKeys.publicKey, identityConfig.apiURL, mapOf(), null)
    val challengeRequest = ChallengeRequest(TozIDEACPChallengeRequest(1))
    val execute = anonymousNoteClient.challengeNote(deriveNoteCreds.noteName, challengeRequest).execute()
    return execute.isSuccessful
  }

  private fun replacePassword(newPassword: String, resultHandler: ResultHandler<Void>) {
    val deriveNoteCreds = Realm.deriveNoteCreds(identityConfig.realmName, identityConfig.username, newPassword, CredentialType.PASSWORD)
    val identityConfigAsString = Realm.mapper.writeValueAsString(identityConfig)
    val storageConfig = client.config.json()
    val data = mapOf("config" to identityConfigAsString, "storage" to storageConfig)
    val noteOptions = NoteOptions().apply {
      noteName = deriveNoteCreds.noteName
      maxViews = -1
      expires = false
      eacp = EACP.Builder().tozIDEACP(TozIDEACP(identityConfig.realmName)).build()
    }
    val idClientData = RecordData(data)
    client.replaceNoteByName(
        idClientData,
        deriveNoteCreds.encryptionKeys.publicKey,
        deriveNoteCreds.signingKeys.publicKey,
        noteOptions
    ) { replacedNoteResult ->
      when {
        replacedNoteResult.isError -> Client.uiExecutor.execute { resultHandler.handle(ErrorResult<Void>(replacedNoteResult.asError().error())) }
        else -> Client.uiExecutor.execute { resultHandler.handle(ValueResult<Void>(null)) }
      }
    }
  }
}

class IdentityClient @JvmOverloads constructor(client: Client, identityConfig: IdentityConfig, val token: AgentToken, certificatePinner: CertificatePinner? = null) : PartialIdentityClient(client, identityConfig) {
  private val identityClient = Retrofit.Builder().apply {
    client(OkHttpClient().newBuilder()
        .apply {
          certificatePinner?.let { pinner ->
            certificatePinner(pinner)
          }
          client.config?.let {
            addInterceptor(Client.TSV1Interceptor(Base64.decodeURL(it.privateSigningKey), Base64.decodeURL(it.publicSigningKey), it.clientId.toString()))
          }
        }.build())
    callbackExecutor(Client.uiExecutor)
    baseUrl(identityConfig.apiURL.toURL())
    addConverterFactory(JacksonConverterFactory.create(Realm.mapper))
  }.build().create<IdentityServiceClient>()

  fun fetchToken(appName: String, resultHandler: ResultHandler<FetchTokenResponse>) {
    Client.backgroundExecutor.run {
      try {
        val tokenResponse = identityClient.tokenRequest(identityConfig.realmName, mapOf("grant_type" to "password", "client_id" to appName)).execute()
        when {
          tokenResponse.isSuccessful -> {
            Client.uiExecutor.execute { resultHandler.handle(ValueResult(tokenResponse.body())) }
          }
          else -> (Client.uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException.find(tokenResponse.code(), tokenResponse.message()))) })
        }
      } catch (e: Exception) {
        (Client.uiExecutor.execute { resultHandler.handle(ErrorResult(E3DBException("An unexpected error occurred during token fetching", e))) })
      }
    }
  }
}


data class AgentToken(val token: String, val tokenType: String, val expiry: Date) {
  companion object {
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
    fun fromJson(json: JsonNode): AgentToken? {
      json["access_token"]?.let { accessToken ->
        json["token_type"]?.let { tokenType ->
          json["expiry"]?.let { expiry ->
            try {
              dateTimeFormat.parse(expiry.asText())?.let { date ->
                return AgentToken(
                    accessToken.asText(),
                    tokenType.asText(),
                    date
                )
              }
            } catch (e: ParseException) {
              return null
            }
          }
        }
      }
      return null
    }
  }
}