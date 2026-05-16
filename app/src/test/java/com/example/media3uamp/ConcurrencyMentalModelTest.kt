package com.example.media3uamp

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.coroutines.resume

class ConcurrencyMentalModelTest {
    @Test
    fun blocking_wait_consumes_threads_first() {
        val taskCount = 100
        val poolSize = 8
        val networkDelayMs = 200L

        val worker = Executors.newFixedThreadPool(poolSize)
        val inFlightBlocking = AtomicInteger(0)
        val maxInFlightBlocking = AtomicInteger(0)
        val threadNames = ConcurrentHashMap.newKeySet<String>()
        val done = CountDownLatch(taskCount)

        val startNs = System.nanoTime()
        repeat(taskCount) { taskId ->
            worker.execute {
                threadNames.add(baseThreadName(Thread.currentThread().name))
                val now = inFlightBlocking.incrementAndGet()
                maxInFlightBlocking.updateAndGet { prev -> max(prev, now) }

                val latch = CountDownLatch(1)
                simulateCallbackNetwork(
                    scheduler = GlobalSchedulers.networkCallbacks,
                    delayMs = networkDelayMs
                ) {
                    latch.countDown()
                }

                latch.await()

                inFlightBlocking.decrementAndGet()
                done.countDown()
                if (taskId == 0) {
                    println("blocking: sample task resumed on ${Thread.currentThread().name}")
                }
            }
        }
        done.await()
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        worker.shutdown()

        println("blocking: taskCount=$taskCount poolSize=$poolSize networkDelayMs=$networkDelayMs")
        println("blocking: elapsedMs=$elapsedMs maxInFlightBlocking=${maxInFlightBlocking.get()}")
        println("blocking: workerThreadsUsed=${threadNames.size} $threadNames")

        assertTrue(maxInFlightBlocking.get() <= poolSize)
        assertTrue(elapsedMs >= (taskCount / poolSize) * networkDelayMs / 2)
    }

    @Test
    fun coroutine_await_tracks_many_waiters_without_many_threads() {
        val taskCount = 100
        val poolSize = 4
        val networkDelayMs = 200L

        val dispatcher = Executors.newFixedThreadPool(poolSize).asCoroutineDispatcher()
        val inFlightAwaiting = AtomicInteger(0)
        val maxInFlightAwaiting = AtomicInteger(0)
        val coroutineThreads = ConcurrentHashMap.newKeySet<String>()

        val startNs = System.nanoTime()
        runBlocking(dispatcher) {
            coroutineScope {
                repeat(taskCount) { taskId ->
                    launch {
                        coroutineThreads.add(baseThreadName(Thread.currentThread().name))
                        val now = inFlightAwaiting.incrementAndGet()
                        maxInFlightAwaiting.updateAndGet { prev -> max(prev, now) }

                        val before = baseThreadName(Thread.currentThread().name)
                        awaitCallbackNetwork(networkDelayMs, GlobalSchedulers.networkCallbacks)
                        val after = baseThreadName(Thread.currentThread().name)

                        if (taskId == 0) {
                            println("coroutine: sample task before=$before after=$after")
                        }

                        inFlightAwaiting.decrementAndGet()
                        coroutineThreads.add(baseThreadName(Thread.currentThread().name))
                    }
                }
            }
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        dispatcher.close()

        println("coroutine: taskCount=$taskCount dispatcherThreads=$poolSize networkDelayMs=$networkDelayMs")
        println("coroutine: elapsedMs=$elapsedMs maxInFlightAwaiting=${maxInFlightAwaiting.get()}")
        println("coroutine: coroutineThreadsUsed=${coroutineThreads.size} $coroutineThreads")

        assertTrue(maxInFlightAwaiting.get() >= taskCount - 5)
        assertTrue(coroutineThreads.size <= poolSize)
        assertTrue(elapsedMs <= networkDelayMs * 6)
    }

    private suspend fun awaitCallbackNetwork(
        delayMs: Long,
        scheduler: ScheduledExecutorService
    ) {
        suspendCancellableCoroutine<Unit> { cont ->
            simulateCallbackNetwork(scheduler, delayMs) {
                if (cont.isActive) cont.resume(Unit) {}
            }
        }
    }

    private fun simulateCallbackNetwork(
        scheduler: ScheduledExecutorService,
        delayMs: Long,
        onComplete: () -> Unit
    ) {
        scheduler.schedule(onComplete, delayMs, TimeUnit.MILLISECONDS)
    }

    private object GlobalSchedulers {
        val networkCallbacks: ScheduledExecutorService =
            ScheduledThreadPoolExecutor(1) { r ->
                Thread(r, "network-callbacks").apply { isDaemon = true }
            }
    }

    private fun baseThreadName(threadName: String): String = threadName.substringBefore(" @")
}
