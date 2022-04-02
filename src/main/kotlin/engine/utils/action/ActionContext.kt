package engine.utils.action

import com.gratedgames.utils.Disposable
import com.gratedgames.utils.Time
import kotlin.concurrent.thread

class ActionContext() : Disposable {
    open class Runner(private var action: Action) {
        fun update(delta: Float): Boolean {
            action.update(delta)
            if (action.isDone)
                action = action.next ?: return false
            return true
        }
    }

    private inner class AsyncRunner(action: Action) : Runner(action) {
        val thread = thread {
            //TODO: Use some sort of threadpool or ExecutorService for this.. However long running tasks have to be considered
            var previousTime = Time.current
            while (!isDisposed) {
                val currentTime = Time.current
                val delta = (currentTime - previousTime).toFloat()
                previousTime = currentTime

                if (!update(delta))
                    break
            }
        }
    }

    private val runners = arrayListOf<Runner>()
    private val processingRunners = arrayListOf<Runner>()
    private var isDisposed = false

    fun update(delta: Float) {
        processingRunners.clear()
        processingRunners.addAll(runners)

        for (runner in processingRunners) {
            if (runner is AsyncRunner)
                continue

            if (!runner.update(delta))
                runners -= runner
        }
    }

    fun start(action: Action) {
        runners += Runner(action.startAction)
    }

    fun startAsync(action: Action) {
        runners += AsyncRunner(action.startAction)
    }

    override fun dispose() {
        isDisposed = true
        for (runner in runners) {
            if (runner is AsyncRunner)
                runner.thread.join()
        }
        runners.clear()
    }
}

fun start(action: Action, context: ActionContext) = context.start(action)

fun startAsync(action: Action, context: ActionContext) = context.startAsync(action)