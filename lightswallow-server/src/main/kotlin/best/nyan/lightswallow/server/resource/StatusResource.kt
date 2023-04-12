package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.RunningSandboxRuntimeRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.service.SandboxRunnableRequestResult
import best.nyan.lightswallow.server.service.SandboxRunnableRequestService
import org.jboss.resteasy.reactive.RestResponse
import javax.enterprise.context.RequestScoped
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/status")
@RequestScoped
class StatusResource(
    private val appConfig: AppConfig,
    private val sandboxRunnableRequestService: SandboxRunnableRequestService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun requestsStatus(@HeaderParam("secret-key") @DefaultValue("") secretKey: String): RestResponse<ServerStatus> {
        if (!appConfig.verifySecretKey(secretKey))
            return RestResponse.status(403)

        return RestResponse.ok(
            ServerStatus(
                serverId = appConfig.serverId,
                threadPoolSize = appConfig.threadPoolSize,
                requestsCount = sandboxRunnableRequestService.runningRequests.size,
                runningRequestCount = sandboxRunnableRequestService.runningRequests
                    .count { it.status == RunningSandboxRuntimeRequest.RunningStatus.RUNNING },
                requestList = sandboxRunnableRequestService.runningRequests
                    .map {
                        RunningSandboxRuntimeRequestListVO(
                            id = it.id,
                            createTime = it.createTime,
                            status = it.status
                        )
                    }
            ))
    }

    @GET
    @Path("/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRequestDetails(
        @HeaderParam("secret-key") @DefaultValue("") secretKey: String,
        @PathParam("requestId") requestId: String
    ): RunningSandboxRuntimeRequestVO? {
        if (!appConfig.verifySecretKey(secretKey))
            return null

        val runningRequest = sandboxRunnableRequestService.runningRequests.find { it.id == requestId } ?: return null
        return RunningSandboxRuntimeRequestVO(
            id = runningRequest.id,
            createTime = runningRequest.createTime,
            status = runningRequest.status,
            request = runningRequest.request
        )
    }

    @GET
    @Path("/{requestId}/result")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRequestResult(
        @HeaderParam("secret-key") @DefaultValue("") secretKey: String,
        @PathParam("requestId") requestId: String
    ): SandboxRunnableRequestResult? {
        if (!appConfig.verifySecretKey(secretKey))
            return null

        val runningRequest = sandboxRunnableRequestService.runningRequests.find { it.id == requestId } ?: return null
        return if (runningRequest.resultFuture.isComplete)
            runningRequest.resultFuture.result()
        else null
    }

    data class ServerStatus(
        val serverId: String,
        val threadPoolSize: Int,
        val requestsCount: Int,
        val runningRequestCount: Int,
        val requestList: List<RunningSandboxRuntimeRequestListVO>
    )

    data class RunningSandboxRuntimeRequestListVO(
        val id: String,
        val createTime: Long,
        val status: RunningSandboxRuntimeRequest.RunningStatus
    )

    data class RunningSandboxRuntimeRequestVO(
        val id: String,
        val createTime: Long,
        val status: RunningSandboxRuntimeRequest.RunningStatus,
        val request: SandboxRunnableRequest
    )

}
