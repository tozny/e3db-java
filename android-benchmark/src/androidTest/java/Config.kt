package com.tozny.e3db.benchmark

import android.support.test.InstrumentationRegistry
import android.util.Log
import dk.ilios.spanner.SpannerConfig
import dk.ilios.spanner.config.RuntimeInstrumentConfig
import dk.ilios.spanner.model.Trial
import dk.ilios.spanner.output.ResultProcessor
import java.io.File
import java.util.concurrent.TimeUnit

object Config {

  @JvmOverloads
  @JvmStatic
  fun config(klass: Class<*>, time: Int = 1, timeUnit: TimeUnit = TimeUnit.SECONDS): SpannerConfig {
    val filesDir = InstrumentationRegistry.getTargetContext().filesDir
    val resultsDir = File(filesDir, "results")

    return SpannerConfig.Builder()
        .saveResults(resultsDir, "${klass.canonicalName}.${System.currentTimeMillis()}.json") // Save results to disk
        .addResultProcessor(object : ResultProcessor {
          override fun processTrial(trial: Trial?) { trial?.let {
            val className = it.scenario().benchmarkSpec().className()
            val methodName = it.scenario().benchmarkSpec().methodName()
            val f : ((Double) -> String) = {"%.3e".format(it / 1000000f) }

            Log.i("BenchmarkResults", "$className#$methodName: min/median/mean/max/99% (ms): ${f(it.min)}/${f(it.median)}/${f(it.mean)}/${f(it.max)}/${f(it.getPercentile(99f))}")
          }}

          override fun close() {

          }
        })
        .addInstrument(RuntimeInstrumentConfig.Builder()
            .warmupTime(500, TimeUnit.MILLISECONDS)
            .timingInterval(time.toLong(), timeUnit)
            .build()) // Configure how benchmark is run/measured
        .build()
  }
}
