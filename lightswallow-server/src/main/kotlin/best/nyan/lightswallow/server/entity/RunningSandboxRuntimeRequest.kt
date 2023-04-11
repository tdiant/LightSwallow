package best.nyan.lightswallow.server.entity

import io.vertx.core.Future

/**
 * Entity of running sandbox runtime request
 */
class RunningSandboxRuntimeRequest(
    /**
     * Origin request
     */
    val request: SandboxRunnableRequest,

    /**
     * Future of the final result
     */
    val resultFuture: Future<SandboxRunnableRequestResult>
) {

    val id get() = request.id
    val createTime: Long = System.currentTimeMillis()

    /**
     * Status for the RunningRequest
     */
    val status: RunningStatus
        get() = when (isPressedRun) {
            false -> RunningStatus.READY
            true -> {
                if (resultFuture.isComplete) RunningStatus.DEAD
                else RunningStatus.RUNNING
            }
        }

    private var isPressedRun = false
    fun pressRunning() {
        isPressedRun = true
    }

    enum class RunningStatus {
        READY, RUNNING, DEAD
    }

}
