/*
 * Copyright 2020 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.effect.unsafe

import scala.concurrent.ExecutionContext

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

abstract private[unsafe] class IORuntimeCompanionPlatform { self: IORuntime.type =>
  def createDefaultComputeExecutionContext(threadPrefix: String = "io-compute-"): (ExecutionContext, () => Unit) = {
    val threadCount = new AtomicInteger(0)
    val executor = Executors.newFixedThreadPool(
      Runtime.getRuntime().availableProcessors(), { (r: Runnable) =>
        val t = new Thread(r)
        t.setName(s"${threadPrefix}-${threadCount.getAndIncrement()}")
        t.setDaemon(true)
        t
      }
    )
    (ExecutionContext.fromExecutor(executor), { () => executor.shutdown() })
  }

  def createDefaultScheduler(threadName: String = "io-scheduler"): (Scheduler, () => Unit) = {
    val scheduler = Executors.newSingleThreadScheduledExecutor { r =>
      val t = new Thread(r)
      t.setName(threadName)
      t.setDaemon(true)
      t.setPriority(Thread.MAX_PRIORITY)
      t
    }
    (Scheduler.fromScheduledExecutor(scheduler), { () => scheduler.shutdown() })
  }

  lazy val global: IORuntime =
    IORuntime(createDefaultComputeExecutionContext()._1, createDefaultScheduler()._1, () => ())
}
