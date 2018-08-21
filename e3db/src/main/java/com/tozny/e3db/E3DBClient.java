package com.tozny.e3db;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines interactions with E3DB, excepting registration.
 *
 * <p>This interface defines all communication with E3DB. Use it to read, write, update, delete and
 * query records. It can also be used to add and remove sharing rules (through the {@code share} and {@code revoke}
 * methods).
 *
 * <h1>Asynchronous Result Handling</h1>
 * Most methods in this class have a {@code void} return type, and instead return results via a callback argument of type {@link ResultHandler}.
 * All E3DB network communication occurs on a background thread (created by the class). Results are delivered via the callback argument given. On Android,
 * results are delivered on the UI thread. On all other platforms, results are delivered on the same background thread that performed the E3DB operation.
 *
 * <p>Note that no E3DB operations have a defined timeout &mdash; your application is responsible for setting timeouts and performing appropriate action.
 *
 * <h2><i>ResultHandler</i> &amp; <i>Result</i> Values</h2>
 * The {@link ResultHandler} callback accepts a {@link Result} value, which signals whether an error occurred or if the operation completed
 * successfully. The {@link Result#isError()} method will return {@code true} if some error
 * occurred. If not, {@link Result#asValue()} will give the value of the operation that occurred.
 *
 * <p>More details are available on the documentation for {@link Result}.
 *
 * <h1>Registering a Client</h1>
 * Registration requires a "token," which associates each
 * client with your Tozny account. Before using the SDK, go to <a href="https://console.tozny.com">https://console.tozny.com</a>,
 * create an account, and go to
 * the "Manage Clients" section. Click the "Create Token" button under
 * the "Client Registration Tokens" heading. Use the token value created to register
 * new clients. Note that this value is not
 * meant to be secret and is safe to embed in your app.
 *
 * <p>The {@link Client#register(String, String, String, ResultHandler)} method can be used to
 * register a client. After registration, save the resulting credentials for use later.
 *
 * <h1>Creating a Client</h1>
 * Use the {@link ClientBuilder} class to create a {@link Client} instance from stored credentials. The
 * convenience method {@link ClientBuilder#fromConfig(Config)} makes it easy to load all client
 * information from one location. The
 * {@link Config} class also provides methods for converting credentials to and from JSON.
 *
 * <p>However, <b>ALWAYS</b> be sure to store client credentials securely. Those are the username,
 * password, and private key used by the {@link Client} instance!
 *
 * <h1>Write, Update and Delete Records</h1>
 *
 * Records can be written with the {@link #write(String, RecordData, Map, ResultHandler)} method,
 * updated with {@link #update(UpdateMeta, RecordData, Map, ResultHandler)}, and deleted with {@link #delete(UUID, String, ResultHandler)}.
 *
 * <h1>Reading &amp; Querying Records</h1>
 *
 * You can read a single record with the {@link #read(UUID, ResultHandler)} method.
 *
 * <p>Multiple records (matching some criteria) can be read using the {@link #query(QueryParams, ResultHandler)} method. You must pass a {@link QueryParams} instance
 * to specify the selection critera for records; use the {@link QueryParamsBuilder} object to build the query.
 *
 * <h2>Pagination</h2>
 * Query result pagination depends on the {@link QueryResponse#last()} value, which returns an index indicating the last record
 * in a given page of results. Pass the {@code last()} value obtained to the {@link QueryParamsBuilder#setAfter}) method to
 * obtain records following the last record.
 *
 * <h1>Sharing</h1>
 * Sharing allows one client to give read access to a set of that client's records to another client, without compromising end-to-end encryption. To share
 * records, a client must know the ID of the client they wish to share with. The SDK does not provide support for looking up client IDs - you will have to
 * implement such support out of band.
 *
 * <p>To share records, use the {@link #share(String, UUID, ResultHandler)} method. To remove sharing access, use the {@link #revoke(String, UUID, ResultHandler)}} method.
 *
 * <h1>Local Encryption &amp; Decryption</h1>
 * The client instance has the ability to encrypt records for local storage, and to decrypt locally-stored records. Locally-encrypted records are
 * always encrypted with a key that can be shared with other clients via E3DB's sharing mechanism.
 *
 * <p>Local encryption (and decryption) requires two steps:
 *
 * <oL>
 *   <li>Create a 'writer key' (for encryption) or obtain a 'reader key' (for decryption).</li>
 *  <li>Call {@link #encryptRecord(String, RecordData, Map, EAKInfo)} (for a new document) or {@link #encryptExisting(LocalRecord, EAKInfo)}
 *  (for an existing {@link LocalRecord} instance); for decryption, call {@link #decryptExisting(EncryptedRecord, EAKInfo)}.</li>
 * </ol>
 *
 * <h1>Obtaining A Key for Local Encryption &amp; Decryption</h1>
 *
 * A writer key can be created by calling {@link #createWriterKey(String, ResultHandler)}; a 'reader key' can be obtained by calling
 * {@link #getReaderKey(UUID, UUID, String, ResultHandler)}. (Note that the client calling {@code getReaderKey} will only receive a key if the writer
 * of those records has given access to the calling client through the `share` operation.)
 *
 * <p>The 'writer key' and 'reader key' returned from the client are both {@link LocalEAKInfo} objects (an implementation of the {@link EAKInfo} interface), which can be persisted to storage via the {@link LocalEAKInfo#encode()}
 * method. Previously-persisted keys can be parsed using the {@link LocalEAKInfo#decode(String)} method.
 *
 * <p>However, note that {@code EAKInfo} objects are encrypted with the client's private key, meaning they cannot be decoded by any other client.
 *
 * <h1>Storing Encrypted Records Locally</h1>
 *
 * Use the {@link LocalEncryptedRecord#encode()} method to convert an encrypted record to a string for storage. You can use
 * the {@link LocalEncryptedRecord#decode(String)} method to convert an encoded record back to an {@code EncryptedRecord}
 * instance.
 *
 * <h1>Document Signing &amp; Verification</h1>
 *
 * <p>Every E3DB client created with this SDK is capable of signing
 * documents and verifying the signature associated with a document.
 * By attaching signatures to documents, clients can be confident in:
 *
 * <ul>
 *   <li>Document integrity - the document's contents have not been altered (because the signature will not match).</li>
 *   <li>Proof-of-authorship - The author of the document held the private signing key associated with the given public key
 *       when the document was created.</li>
 * </ul>
 *
 * <p>Use the {@link #sign(Signable)} method to sign a document. Note that {@link Record}, {@link LocalEncryptedRecord}, and {@link LocalRecord}
 * all implement the {@link Signable} interface, and thus can be signed.
 *
 * To verify a signed document, use the {@link #verify(SignedDocument, String)} method. Note that the {@link LocalEncryptedRecord} class
 * implements {@link SignedDocument} and thus always has a signature attached that can be verified.
 */
public interface E3DBClient {
  /**
   * The ID of this client.
   *
   * @return clientId.
   */
  UUID clientId();

  /**
   * Write a new record.
   *
   * @param type Describes the type of the record (e.g., "contact_info", "credit_card", etc.).
   * @param fields Values to encrypt and store.
   * @param plain Additional, user-defined metadata that will <b>NOT</b> be encrypted. Can be null.
   * @param handleResult Result of the operation. If successful, returns the newly written record .
   */
  void write(String type, RecordData fields, Map<String, String> plain, ResultHandler<Record> handleResult);

  /**
   * Replace the given record with new data and plaintext metadata.
   *
   * <p>The {@code updateMeta} argument is only used to obtain
   * information about the record to replace. No metadata updates by
   * the client are allowed.
   * @param updateMeta Metadata describing the record to update. Can be obtained
   *                   from an existing {@link RecordMeta} instance using {@link LocalUpdateMeta#fromRecordMeta(RecordMeta)}.
   * @param fields Field names and values. Wrapped in a {@link
   *               RecordData} instance to prevent confusing with the
   *               {@code plain} parameter.
   * @param plain Any metadata associated with the record that will
   *              <b>NOT</b> be encrypted. If {@code null}, existing
   *              metadata will be removed.
   * @param handleResult If the update succeeds, returns the updated
   *                     record. If the update fails due to a version
   *                     conflict, the value passed to the {@link
   *                     ResultHandler#handle(Result)}} method return
   *                     an instance of {@link E3DBVersionException}
   *                     when {@code asError().error()} is called.
   */
  void update(UpdateMeta updateMeta, RecordData fields, Map<String, String> plain, ResultHandler<Record> handleResult);

  /**
   * Deletes a given record.
   *
   * @param recordId ID of the record to delete. Obtained from {@link RecordMeta#recordId()}.
   * @param version Version associated with the record. Obtained from {@link RecordMeta#version()}.
   * @param handleResult If the deletion succeeds, returns no useful information. If the delete fails due to a version conflict, the value passed to the {@link ResultHandler#handle(Result)}} method return an instance of
   * {@link E3DBVersionException} when {@code asError().error()} is called.
   */
  void delete(UUID recordId, String version, ResultHandler<Void> handleResult);

  /**
   * Read a record.
   *
   * @param recordId ID of the record to read. Especially useful with {@code query} results that do not include
   *                 the actual data.
   * @param handleResult If successful, return the record read.
   */
  void read(UUID recordId, ResultHandler<Record> handleResult);

  /**
   * Get a list of records matching some criteria.
   *
   * <p>By default, results are limited to 50 records and do not include encrypted data (a separate
   * read must be made to get the data for a record).
   *
   * @param params The criteria to filter records by. Use the {@link QueryParamsBuilder} class to make this object.
   * @param handleResult If successful, returns a page of results. The {@link QueryResponse#last()} method can be used
   *                     in conjunction with the {@link QueryParamsBuilder#setAfter(long)} method to implement pagination.
   */
  void query(QueryParams params, ResultHandler<QueryResponse> handleResult);

  /**
   * Give another client the ability to read records.
   *
   * <p>This operation gives read access for records of {@code type}, written by this client, to the
   * the recipient specified by {@code readerId}.
   *
   * @param type The type of records to grant access to.
   * @param readerId ID of client to give access to.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  void share(String type, UUID readerId, ResultHandler<Void> handleResult);

  /**
   * Remove permission for another client to read records.
   *
   * <p>This operation removes previously granted permission for the client specified by {@code readerId} to
   * read records, written by this client, of type {@code type}.
   *
   * @param type The type of records to remove access to.
   * @param readerId ID of client to remove access from.
   * @param handleResult If successful, returns no useful information (except that the operation
   *                     completed).
   */
  void revoke(String type, UUID readerId, ResultHandler<Void> handleResult);

  /**
   * Get a list of record types shared with this client.
   *
   * <p>This operation lists all record types shared with this client, as well as the client (writer)
   * sharing those records.
   * @param handleResult If successful, returns a list of records types shared with this client. The resulting list may be empty but never null.
   */
  void getIncomingSharing(ResultHandler<List<IncomingSharingPolicy>> handleResult);

  /**
   * Get a list of record types shared by this client.
   *
   * <p>This operation returns a list of record types shared by this client, including the
   * client (reader) that the records are shared with.
   * @param handleResult If successful, returns a list of record types that this client has shared. The resulting list may be empty but will never be null.
   */
  void getOutgoingSharing(ResultHandler<List<OutgoingSharingPolicy>> handleResult);

  /**
   * Creates (or retrieves) a key that can be used to locally encrypt records.
   *
   * @param type Type of the records to encrypt.
   * @param handleResult Handle the LocalEAKInfo object retrieved.
   */
  void createWriterKey(String type, ResultHandler<LocalEAKInfo> handleResult);

  /**
   * Retrieve a key for reading a shared record.
   * @param writerId ID of the client that wrote the record.
   * @param userId ID of the user associated with the record (normally equal to {@code writerId}).
   * @param type Type of record that was shared.
   * @param handleResult Handle the LocalEAKInfo object retrieved.
   */
  void getReaderKey(UUID writerId, UUID userId, String type, ResultHandler<LocalEAKInfo> handleResult);

  /**
   * Decrypt a locally-encrypted record.
   *
   * @param record The record to decrypt.
   * @param eakInfo The key to use for decrypting.
   * @throws E3DBVerificationException Thrown when verification of the signature fails.
   * @return The decrypted record.
   */
  LocalRecord decryptExisting(EncryptedRecord record, EAKInfo eakInfo) throws E3DBVerificationException;

  /**
   * Sign &amp; encrypt an existing record for local storage.
   *
   * @param record The record to encrypt.
   * @param eakInfo The key to use for encrypting.
   * @return The encrypted record.
   */
  LocalEncryptedRecord encryptExisting(LocalRecord record, EAKInfo eakInfo);

  /**
   * Sign &amp; encrypt a record for local storage.
   *
   * @param type The type of the record.
   * @param data Fields to encrypt.
   * @param plain Plaintext metadata which will be stored with the record.
   * @param eakInfo The key to use for encrypting.
   * @return The encrypted record.
   */
  LocalEncryptedRecord encryptRecord(String type, RecordData data, Map<String, String> plain, EAKInfo eakInfo);

  /**
   * Derives a signature using this client's private Ed25519 key and the document given.
   *
   * @param document The document to sign. Consider using {@link LocalRecord}, which implements the
   *                 {@link Signable} interface.
   * @param <T> T.
   * @return A {@link SignedDocument} instance, holding the document given and a signature
   * for it.
   */
  <T extends Signable> SignedDocument<T> sign(T document);

  /**
   * Verifies that the signature attached to the document was:
   *
   * <ul>
   * <li>Produced by the holder of the private key associated with the {@code publicSigningKey} given.</li>
   * <li>Derived from the document given.
   * </ul>
   *
   * @param signedDocument Document and signature to verify.
   * @param publicSigningKey Public portion of the Ed25519 key used to produce the signature, as a Base64URL-encoded
   *                         string.
   * @return {@code true} if the signature matches; {@code false} otherwise.
   */
  boolean verify(SignedDocument signedDocument, String publicSigningKey);
}
