package com.tozny.e3db;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

interface Client {
    /**
     * The ID of this client.
     *
     * @return clientId.
     */
    UUID clientId();

    /**
     * Write the given file to E3DB. Intended for files from 5MB up to 5GB in size. The contents of the file are
     * encrypted before being uploaded.
     *
     * @param type Type of the record. Cannot be {@code null} or blank.
     * @param file Path to the file to upload. Cannot be {@code null}.
     * @param plain Plaintext meta associated with the file. Can be {@code null}.
     * @param handleResult Handles the result of the write.
     */
    void writeFile(String type, File file, Map<String, String> plain, ResultHandler<RecordMeta> handleResult);

    /**
     * Read the file associated with the given record from the server.
     *
     * @param recordId ID of the record. Cannot be {@code null}.
     * @param dest Destination to write the decrypted file to. If the file exists, it will be truncated.
     * @param handleResult Handles the result of the operation. If the operation completes successfully, the
     *                     destination will hold the contents of the unencrypted file.
     */
    void readFile(UUID recordId, File dest, ResultHandler<RecordMeta> handleResult);

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
     * Share records written by the given writer with the given reader.
     *
     * <p>This method should be called by a client authorized to share on behalf of the given writer.</p>
     *
     * @param writer ID of the client that produces the records.
     * @param type The type of records to grant access to.
     * @param readerId ID of client which will be allowed to read the records.
     * @param handleResult If successful, returns no useful information (except that the operation
     *                     completed).
     */
    void shareOnBehalfOf(WriterId writer, String type, UUID readerId, ResultHandler<Void> handleResult);

    /**
     * Give another client the ability to read records.
     *
     * <p>This operation gives read access for records of {@code type}, written by this client, to the
     * the recipient specified by {@code readerId}.
     *
     * @param type The type of records to grant access to.
     * @param readerId ID of client which will be allowed to read the records.
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
     * Remove permission for the given reader to read records written by the writer.
     *
     * <p>This method should be called by a client authorized to share on behalf of the writer.
     *
     * @param writerId Writer that produces the records
     * @param type Record type to revoke permission for.
     * @param readerId Reader who's permission will be revoked.
     * @param handleResult If successful, returns no useful information (except that the operation
     *                     completed).
     */
    void revokeOnBehalfOf(WriterId writerId, String type, UUID readerId, ResultHandler<Void> handleResult);

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

    /**
     * Adds an authorizer for records written by this client.
     *
     * <p>Calling this method will grant permission for the "{@code authorizer}" client to allow
     * <b>other</b> clients to read records of the given type, written by this client.
     *
     * <p>The authorizer client itself will not be able to read records; it will only be able to grant
     * permission for other clients to do so.
     *
     * @param authorizer ID of the authorizer. Canot be {@code null}.
     * @param recordType Record type to give authorizer ability to share. Canot be {@code null}.
     * @param handler Handles result of the call.
     */
    void addAuthorizer(UUID authorizer, String recordType, ResultHandler<Void> handler);

    /**
     * Remove the given client's ability to share any record type written by this client.
     *
     * <p>This method removes the permission granted by {@link #addAuthorizer(UUID, String, ResultHandler)},
     * for all record types written by this client.
     *
     * <p>No error occurs if the client did not have permission in the first place.
     *
     * @param authorizer ID of the authorizer. Cannot be {@code null}.
     * @param handler Handles the result of the operation.
     */
    void removeAuthorizer(UUID authorizer, ResultHandler<Void> handler);

    /**
     * Remove the given client's ability to share the specific type of record given.
     *
     * <p>This method removes the permission granted by {@link #addAuthorizer(UUID, String, ResultHandler)},
     * for the specific record type identified.
     *
     * <p>No error occurs if the client did not have permission in the first place.
     *
     * @param authorizer ID of the authorizer. Cannot be {@code null}.
     * @param recordType Type of record. Cannot be blank or {@code null}.
     * @param handler Handles the result of the operation.
     */
    void removeAuthorizer(UUID authorizer, String recordType, ResultHandler<Void> handler);

    /**
     * Lists all the clients (and associated record types) that this client has authorized
     * to share on its behalf.
     *
     * <p>This method allows the client to inspect all other clients that have permission to
     * share records on this client's behalf.
     *
     * @param handler Handles result of the call.
     */
    void getAuthorizers(ResultHandler<List<AuthorizerPolicy>> handler);

    /**
     * Lists all writers (and associated record types) that have authorized this client to
     * share data on their behalf.
     *
     * <p>This method allows the client to inspect the list of data producer's that have
     * authorized the client to share records on their behalf.
     *
     * @param handler Handles result of the call.
     */
    void getAuthorizedBy(ResultHandler<List<AuthorizerPolicy>> handler);
}
