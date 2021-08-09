package com.tozny.e3db

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import retrofit2.Call
import retrofit2.http.*
import java.util.*


data class RegisterIdentityRequest constructor(
    @JsonProperty("realm_registration_token") val realmRegistrationToken: String,
    @JsonProperty("realm_name") val realmName: String,
    @JsonProperty("identity") val identity: IdentityInfo
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IdentityInfo constructor(
    @JsonProperty("realm_name") val realmName: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("public_key") val publicKey: Curve25519PublicKey,
    @JsonProperty("signing_key") val signingKey: Ed25519PublicKey,
    @JsonProperty("first_name") val firstName: String?,
    @JsonProperty("last_name") val lastName: String?,
    @JsonProperty("api_key_id") val apiKeyID: String?,
    @JsonProperty("api_secret_key") val apiKeySecret: String?,
    @JsonProperty("id") val userId: Int?,
    @JsonProperty("tozny_id") val toznyClientID: UUID?
)

data class Curve25519PublicKey constructor(@JsonProperty("curve25519") val curve22519Key: String)
data class Ed25519PublicKey constructor(@JsonProperty("ed25519") val ed22519Key: String)

data class RegisterIdentityResponse constructor(
    @JsonProperty("identity") val identityInfo: IdentityInfo,
    @JsonProperty("realm_broker_identity_tozny_id") val realmBrokerIdentityToznyId: UUID?
)

data class LoginRequest(
    @JsonProperty("username") val username: String,
    @JsonProperty("realm_name") val realmName: String,
    @JsonProperty("app_name") val appName: String,
    @JsonProperty("login_style") val loginStyle: LoginStyles = LoginStyles.API
)


data class CompleteLoginAction(
    @JsonProperty("login_action") val loginAction: Boolean,
    @JsonProperty("type") val loginActionType: String,
    @JsonProperty("action_url") val actionURL: String,
    @JsonProperty("fields") val fields: Map<String, FieldType>,
    @JsonProperty("context") val context: Map<String, String>,
    @JsonProperty("content_type") val contentType: String,
    @JsonProperty("message") val errorMessage: LoginActionErrorMessage?
)

data class LoginActionErrorMessage(
    @JsonProperty("summary") val summary: String?,
    @JsonProperty("type") val type: String?,
    @JsonProperty("warning") val warning: Boolean?,
    @JsonProperty("success") val success: Boolean?,
    @JsonProperty("error") val error: Boolean?
)

data class LoginRedirectRequest(
    @JsonProperty("realm_name") val realmName: String,
    @JsonProperty("session_code") val sessionCode: String,
    @JsonProperty("execution") val execution: String,
    @JsonProperty("tab_id") val tabId: String,
    @JsonProperty("client_id") val clientID: String,
    @JsonProperty("auth_session_id") val authSessionID: String
)

enum class LoginStyles {
  @JsonProperty("api")
  API,
}

enum class FieldType {
  @JsonProperty("STRING") STRING,
  @JsonProperty("NUMBER") NUMBER,
  @JsonProperty("BOOLEAN") BOOLEAN
}

interface IdentityServiceClient {
  //This does not require signed headers
  @POST("v1/identity/register")
  fun registerIdentity(@Body request: RegisterIdentityRequest): Call<RegisterIdentityResponse>

  // This method requires a TSV1 http client
  @POST("v1/identity/login")
  fun loginIdentity(@Body request: LoginRequest): Call<JsonNode>

  //This does not require signed headers
  @POST("/auth/realms/{realm}/protocol/openid-connect/auth")
  @FormUrlEncoded
  fun sessionRequest(@Path("realm") realmName: String, @FieldMap fields: Map<String, String>): Call<CompleteLoginAction>

  // This method requires a TSV1 http client
  @POST("/auth/realms/{realm}/protocol/openid-connect/token")
  @FormUrlEncoded
  fun tokenRequest(@Path("realm") realmName: String, @FieldMap fields: Map<String, String>): Call<FetchTokenResponse>


  // This method requires a TSV1 http client
  @POST("/v1/identity/tozid/redirect")
  fun loginredirect(@Body request: LoginRedirectRequest): Call<JsonNode>

  @GET("v1/identity/info/realm/{realm}")
  fun getPublicRealmInfo(@Path("realm") realmName: String): Call<PublicRealmInfo>
}

interface BrokerClient {
  @POST("challenge")
  fun brokerChallenge(@Body initiateChallengeRequest: InitiateChallengeRequest): Call<Void>

  @POST("login")
  fun completeBrokerLogin(@Body body: CompleteBrokerLoginRequest): Call<CompleteBrokerLoginResponse>
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FetchTokenResponse(@JsonProperty("access_token") val accessToken: String?,
                              @JsonProperty("expires_in") val expiresIn: Int,
                              @JsonProperty("refresh_expires_in") val refreshExpiresIn: Int,
                              @JsonProperty("refresh_token") val refreshToken: String?,
                              @JsonProperty("token_type") val tokenType: String?)

data class PublicRealmInfo(@JsonProperty("name") val name: String,
                           @JsonProperty("broker_id") val brokerId: UUID,
                           @JsonProperty("domain") val domain: String)

data class InitiateChallengeRequest(@JsonProperty("username") val username: String, @JsonProperty("action") val action: String = "challenge")

data class CompleteBrokerLoginRequest(
    @JsonProperty("auth_response") val authResponse: Map<String, Any>,
    @JsonProperty("note_id") val noteId: UUID,
    @JsonProperty("public_key") val publicKey: String,
    @JsonProperty("signing_key") val signingKey: String,
    @JsonProperty("action") val action: String = "login"
)

data class CompleteBrokerLoginResponse(
    @JsonProperty("transferId") val transferId: UUID
)