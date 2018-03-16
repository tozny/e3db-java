package com.tozny.e3db.benchmark

import com.tozny.e3db.*
import okio.ByteString
import org.openjdk.jmh.annotations.*
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


fun randomBytes(numBytes: Int): String? {
  val result = ByteArray(numBytes)
  SecureRandom().nextBytes(result)
  return ByteString.of(*result).base64()
}

fun registerClient(): Pair<Client, String> {
  val token = "e791f9fbb063f7496886e906eb568b84d20825593080c7a095fa1a1f49ee40c5"
  val name = "${UUID.randomUUID()}"
  val latch1 = CountDownLatch(1)
  var client: Client? = null
  var publicSigningKey: String? = null

  Client.register(token, name, "https://api.e3db.com") {
    client = ClientBuilder().fromConfig(it.asValue()).build()
    publicSigningKey = it.asValue().publicSigningKey
    latch1.countDown()
  }

  if (!latch1.await(5, TimeUnit.SECONDS))
    throw Exception("Failed to register in 5 seconds.")

  return Pair(client!!, publicSigningKey!!)
}

fun createKey(client1: Client, recordType: String): EAKInfo {
  val latch2 = CountDownLatch(1)
  var writer1: EAKInfo? = null

  client1.createWriterKey(recordType, { result ->
    run {
      writer1 = result.asValue()
      latch2.countDown()
    }
  })

  if (!latch2.await(5, TimeUnit.SECONDS))
    throw Exception("Failed to create writer key in 5 seconds.")

  return writer1!!
}


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 2,time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class CryptoBenchmark {

  @State(Scope.Benchmark)
  open class Setup {
    val recordType = "${UUID.randomUUID()}"

    val client: Client
    val publicSigningKey: String
    init {
      val (c, k) = registerClient()
      client = c
      publicSigningKey = k
    }
    val key: EAKInfo = createKey(client, recordType)

    val byte = randomBytes(1)
    val bytes1KB = randomBytes(1000)
    val bytes250KB = randomBytes(250_000)
    val bytes500KB = randomBytes(500_000)
    val bytes1MB = randomBytes(1_000_000)
    val bytes2MB = randomBytes(2_000_000)
    val bytes8MB = randomBytes(8_000_000)
    val bytes16MB = randomBytes(16_000_000)

    val meta = LocalMeta(client.clientId(), client.clientId(), recordType, null)
    val record1B = LocalRecord(mapOf(Pair("data", byte)), meta)
    val record1KB = LocalRecord(mapOf(Pair("data", bytes1KB)), meta)
    val record250KB = LocalRecord(mapOf(Pair("data", bytes250KB)), meta)
    val record500KB = LocalRecord(mapOf(Pair("data", bytes500KB)), meta)
    val record1MB = LocalRecord(mapOf(Pair("data", bytes1MB)), meta)
    val record2MB = LocalRecord(mapOf(Pair("data", bytes2MB)), meta)
    val record8MB = LocalRecord(mapOf(Pair("data", bytes8MB)), meta)
    val record16MB = LocalRecord(mapOf(Pair("data", bytes16MB)), meta)
  }

  @State(Scope.Benchmark)
  open class DecryptFixtures {
    val recordType = "${UUID.randomUUID()}"
    val client: Client
    val publicSigningKey: String
    init {
      val (c, k) = registerClient()
      client = c
      publicSigningKey = k
    }
    val key: EAKInfo = createKey(client, recordType)

    val rec1B: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(1)))), null, key)
    val rec1KB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(1000)))), null, key)
    val rec250KB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(250_000)))), null, key)
    val rec500KB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(500_000)))), null, key)
    val rec1MB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(1_000_000)))), null, key)
    val rec2MB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(2_000_000)))), null, key)
    val rec8MB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(8_000_000)))), null, key)
    val rec16MB: Record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", randomBytes(16_000_000)))), null, key)

    val signed1B = client.sign(rec1B)
    val signed1KB = client.sign(rec1KB)
    val signed250KB = client.sign(rec250KB)
    val signed500KB = client.sign(rec500KB)
    val signed1MB = client.sign(rec1MB)
    val signed2MB = client.sign(rec2MB)
    val signed8MB = client.sign(rec8MB)
    val signed16MB = client.sign(rec16MB)
  }

  @Benchmark
  fun decrypt1B(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec1B, setup.key)
  }

  @Benchmark
  fun verify1B(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed1B, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt1KB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec1KB, setup.key)
  }

  @Benchmark
  fun verify1KB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed1KB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt250KB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec250KB, setup.key)
  }

  @Benchmark
  fun verify250KB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed250KB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt500KB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec500KB, setup.key)
  }

  @Benchmark
  fun verify500KB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed500KB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt1MB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec1MB, setup.key)
  }

  @Benchmark
  fun verify1MB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed1MB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt2MB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec2MB, setup.key)
  }

  @Benchmark
  fun verify2MB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed2MB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt8MB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec8MB, setup.key)
  }

  @Benchmark
  fun verify8MB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed8MB, setup.publicSigningKey)
  }

  @Benchmark
  fun decrypt16MB(setup: DecryptFixtures): Record? {
    return setup.client.decryptExisting(setup.rec16MB, setup.key)
  }

  @Benchmark
  fun verify16MB(setup: DecryptFixtures): Boolean? {
    return setup.client.verify(setup.signed16MB, setup.publicSigningKey)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  fun encrypt1B(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.byte))), null, setup.key)
  }

  @Benchmark
  fun sign1B(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record1B)
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  fun encrypt1KB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes1KB))), null, setup.key)
  }

  @Benchmark
  fun sign1KB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record1KB)
  }

  @Benchmark
  fun encrypt250KB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes250KB))), null, setup.key)
  }

  @Benchmark
  fun sign250KB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record250KB)
  }

  @Benchmark
  fun encrypt500KB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes500KB))), null, setup.key)
  }

  @Benchmark
  fun sign500KB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record500KB)
  }

  @Benchmark
  fun encrypt1MB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes1MB))), null, setup.key)
  }

  @Benchmark
  fun sign1MB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record1MB)
  }

  @Benchmark
  fun encrypt2MB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes2MB))), null, setup.key)
  }

  @Benchmark
  fun sign2MB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record2MB)
  }

  @Benchmark
  fun encrypt8MB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes8MB))), null, setup.key)
  }

  @Benchmark
  fun sign8MB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record8MB)
  }

  @Benchmark
  fun encrypt16MB(setup: Setup): Record? {
    return setup.client.encryptRecord(setup.recordType, RecordData(mapOf(Pair("data", setup.bytes16MB))), null, setup.key)
  }

  @Benchmark
  fun sign16MB(setup:Setup): SignedDocument<Record> {
    return setup.client.sign(setup.record16MB)
  }
}
