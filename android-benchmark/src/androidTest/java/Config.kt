/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

package com.tozny.e3db.benchmark

import androidx.test.platform.app.InstrumentationRegistry
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
    val filesDir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
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
