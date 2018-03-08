package com.tozny.e3db.benchmark

import android.util.Base64
import java.security.SecureRandom
import com.tozny.e3db.*
import dk.ilios.spanner.*
import dk.ilios.spanner.junit.SpannerRunner
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun randomBytes(numBytes: Int): String? {
  val result = ByteArray(numBytes)
  SecureRandom().nextBytes(result)
  return Base64.encodeToString(result, Base64.NO_WRAP or Base64.URL_SAFE)
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

// From https://github.com/renatoathaydes/kotlin-hidden-costs-benchmark
class BlackHole {
  private val values = arrayOfNulls<Any>(16)
  private var i = 0

  fun consume(value: Any) {
    this.values[i] = value
    i = (i + 1) % 16
  }
}

@RunWith(SpannerRunner::class)
open class CryptoBenchmark1B {

  @BenchmarkConfiguration
  @JvmField
  var configuration = Config.config(javaClass, 5, TimeUnit.SECONDS)

  val client: Client
  val publicSigningKey: String
  init {
    val (c, k) = registerClient()
    client = c
    publicSigningKey = k
  }

  val recordType = UUID.randomUUID().toString()
  val key = createKey(client, recordType)

  val blackhole = BlackHole()
  private val TAG = "CryptoBenchmark"

  lateinit var bytes : String
  lateinit var record: Record

  @BeforeExperiment
  open fun setup() {
    bytes =  randomBytes(1)!!
    record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key)
  }

  @Benchmark
  open fun decrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.decryptExisting(record, key))
  }

  @Benchmark
  open fun encrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key))
  }

  @Benchmark
  open fun sign(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    for(i in 0 until reps)
      consume(client.sign(localRecord))
  }

  @Benchmark
  open fun verify(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    val signed = client.sign(localRecord)
    for(i in 0 until reps)
      consume(client.verify(signed, publicSigningKey))
  }
}

@RunWith(SpannerRunner::class)
open class CryptoBenchmark1KB {

  @BenchmarkConfiguration
  @JvmField
  var configuration = Config.config(javaClass, 5, TimeUnit.SECONDS)

  val client: Client
  val publicSigningKey: String
  init {
    val (c, k) = registerClient()
    client = c
    publicSigningKey = k
  }

  val recordType = UUID.randomUUID().toString()
  val key = createKey(client, recordType)

  val blackhole = BlackHole()
  private val TAG = "CryptoBenchmark"

  var bytes: String? = null
  var record: Record? = null

  @BeforeExperiment
  open fun setup() {
    bytes =  randomBytes(1_000)!!
    record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key)
  }

  @AfterExperiment
  open fun teardown() {
    bytes = null
    record = null
  }

  @Benchmark
  open fun decrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.decryptExisting(record, key))
  }

  @Benchmark
  open fun encrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key))
  }

  @Benchmark
  open fun sign(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    for(i in 0 until reps)
      consume(client.sign(localRecord))
  }

  @Benchmark
  open fun verify(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    val signed = client.sign(localRecord)
    for(i in 0 until reps)
      consume(client.verify(signed, publicSigningKey))
  }
}

@RunWith(SpannerRunner::class)
open class CryptoBenchmark1MB {

  @BenchmarkConfiguration
  @JvmField
  var configuration = Config.config(javaClass, 5, TimeUnit.SECONDS)

  val client: Client
  val publicSigningKey: String
  init {
    val (c, k) = registerClient()
    client = c
    publicSigningKey = k
  }

  val recordType = UUID.randomUUID().toString()
  val key = createKey(client, recordType)

  val blackhole = BlackHole()
  private val TAG = "CryptoBenchmark"

  var bytes: String? = null
  var record: Record? = null

  @BeforeExperiment
  open fun setup() {
    bytes =  randomBytes(1_000_000)!!
    record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key)
  }

  @AfterExperiment
  open fun teardown() {
    bytes = null
    record = null
  }

  @Benchmark
  open fun decrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.decryptExisting(record, key))
  }

  @Benchmark
  open fun encrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key))

  }

  @Benchmark
  open fun sign(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    for(i in 0 until reps)
      consume(client.sign(localRecord))
  }

  @Benchmark
  open fun verify(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    val signed = client.sign(localRecord)
    for(i in 0 until reps)
      consume(client.verify(signed, publicSigningKey))
  }
}

@RunWith(SpannerRunner::class)
open class CryptoBenchmark4MB {

  @BenchmarkConfiguration
  @JvmField
  var configuration = Config.config(javaClass, 5, TimeUnit.SECONDS)

  val client: Client
  val publicSigningKey: String
  init {
    val (c, k) = registerClient()
    client = c
    publicSigningKey = k
  }

  val recordType = UUID.randomUUID().toString()
  val key = createKey(client, recordType)

  val blackhole = BlackHole()
  private val TAG = "CryptoBenchmark"

  var bytes: String? = null
  var record: Record? = null

  @BeforeExperiment
  open fun setup() {
    bytes =  randomBytes(4_000_000)!!
    record = client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key)
  }

  @AfterExperiment
  open fun teardown() {
    bytes = null
    record = null

  }

  @Benchmark
  open fun decrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.decryptExisting(record, key))
  }

  @Benchmark
  open fun encrypt(reps: Int): BlackHole = blackhole.apply {
    for(i in 0 until reps)
      consume(client.encryptRecord(recordType, RecordData(mapOf(Pair("data", bytes))), null, key))
  }

  @Benchmark
  open fun sign(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    for(i in 0 until reps)
      consume(client.sign(localRecord))
  }

  @Benchmark
  open fun verify(reps: Int): BlackHole = blackhole.apply {
    val localRecord = LocalRecord(mapOf(Pair("data", bytes)), LocalMeta(client.clientId(), client.clientId(), recordType, null))
    val signed = client.sign(localRecord)
    for(i in 0 until reps)
      consume(client.verify(signed, publicSigningKey))
  }
}