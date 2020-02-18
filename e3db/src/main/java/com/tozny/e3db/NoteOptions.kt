package com.tozny.e3db

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

class NoteOptions {
  //Premium options
  var clientID: UUID? = null
  @JvmField
  var maxViews = 0
  @JvmField
  var noteName: String? = null
  @JvmField
  var expiration: Instant? = null
  @JvmField
  var expires = false
  @JvmField
  var eacp: EACP? = null
  //Non-premium option
  @JvmField
  var noteType: String? = null
  @JvmField
  var plain: Map<String, String>? = null
  @JvmField
  var fileMeta: Map<String, String>? = null


  constructor(clientID: UUID?, maxViews: Int, noteName: String?, expiration: Instant?, expires: Boolean, noteType: String?, plain: Map<String, String>?, fileMeta: Map<String, String>?, eacp: EACP?) {
    this.clientID = clientID
    this.maxViews = maxViews
    this.noteName = noteName
    this.expiration = expiration
    this.expires = expires
    this.noteType = noteType
    this.plain = plain
    this.fileMeta = fileMeta
    this.eacp = eacp
  }

  constructor() {}

}

class EACP constructor(
    @JsonProperty("email_eacp") val emailEACP: EmailEACP? = null,
    @JsonProperty("last_access_eacp") val lastAccessEACP: LastAccessEACP? = null,
    @JsonProperty("tozny_otp_eacp") val toznyOTPEACP: ToznyOTPEACP? = null,
    @JsonProperty("tozid_eacp") val tozIDEACP: TozIDEACP? = null
) {
  data class Builder(
      var emailEACP: EmailEACP? = null,
      var lastAccessEACP: LastAccessEACP? = null,
      var toznyOTPEACP: ToznyOTPEACP? = null,
      var tozIDEACP: TozIDEACP? = null
  ) {
    fun emailEACP(emailEACP: EmailEACP?) = apply { this.emailEACP = emailEACP }
    fun lastAccessEACP(lastAccessEACP: LastAccessEACP?) = apply { this.lastAccessEACP = lastAccessEACP }
    fun toznyOTPEACP(toznyOTPEACP: ToznyOTPEACP?) = apply { this.toznyOTPEACP = toznyOTPEACP }
    fun tozIDEACP(tozIDEACP: TozIDEACP?) = apply { this.tozIDEACP = tozIDEACP }
    fun build(): EACP = EACP(this.emailEACP, this.lastAccessEACP, this.toznyOTPEACP, this.tozIDEACP)
  }
}

data class EmailEACP constructor(
    @JsonProperty("email_address") val emailAddress: String? = null,
    @JsonProperty("template") val template: String? = null,
    @JsonProperty("provider_link") val providerLink: String? = null,
    @JsonProperty("template_fields") val templateFields: Map<String, String>? = HashMap(),
    @JsonProperty("default_expiration_minutes") val DefaultExpirationMinutes: Int = 0
)

data class LastAccessEACP(@JsonProperty("last_read_note_id") val lastReadNoteID: UUID? = null)

data class ToznyOTPEACP constructor(@JsonProperty("include") val include: Boolean = false)

data class TozIDEACP(@JsonProperty("realm_name") val realmName: String? = null)

data class ChallengeRequest(@JsonProperty("tozid_eacp") val tozIDChallenge: TozIDEACPChallengeRequest? = null, @JsonProperty("email_eacp") val emailChallenge: EmailEACPChallengeRequest? = null)

data class TozIDEACPChallengeRequest(@JsonProperty("expiry_seconds") val expirySeconds: Int)

data class EmailEACPChallengeRequest(@JsonProperty("template_name") val templateName: String?, @JsonProperty("expiry_minutes") val expiryMinutes: Int)