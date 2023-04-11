package best.nyan.lightswallow.server.service

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.RunningSandboxRuntimeRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequestResult
import io.vertx.core.Promise
import java.util.concurrent.Executors
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class SandboxRunnableRequestService(
    private val appConfig: AppConfig
) {

    private val threadPool = Executors.newFixedThreadPool(appConfig.threadPoolSize)

    /**
     * Running requests
     */
    val runningRequests = mutableListOf<RunningSandboxRuntimeRequest>()

    fun runRequest(request: SandboxRunnableRequest): RunningSandboxRuntimeRequest? {
        //Check if queue fulled
        if (runningRequests.count { it.status == RunningSandboxRuntimeRequest.RunningStatus.RUNNING } > appConfig.threadPoolSize ||
            runningRequests.count { it.status == RunningSandboxRuntimeRequest.RunningStatus.READY } > appConfig.threadPoolSize * 2)
            return null

        val futureResultPromise = Promise.promise<SandboxRunnableRequestResult>()
        val runningRequest = RunningSandboxRuntimeRequest(request, futureResultPromise.future())

        threadPool.execute {
            cleanRunningRequests()
            runningRequest.pressRunning()
            val finalResult = request.runAndWait(appConfig.serverId)
            futureResultPromise.complete(finalResult)
        }

        runningRequests.add(runningRequest)

        return runningRequest
    }

    private fun cleanRunningRequests() {
        runningRequests.removeIf {
            System.currentTimeMillis() - it.createTime > 300000 && //5min
                    it.status == RunningSandboxRuntimeRequest.RunningStatus.DEAD
        }
    }

}
