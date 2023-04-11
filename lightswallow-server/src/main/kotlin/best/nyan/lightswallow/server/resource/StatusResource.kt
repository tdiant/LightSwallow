package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.RunningSandboxRuntimeRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.service.SandboxRunnableRequestService
import javax.enterprise.context.RequestScoped
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/status")
@RequestScoped
class StatusResource(
    private val appConfig: AppConfig,
    private val sandboxRunnableRequestService: SandboxRunnableRequestService
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun requestsStatus(): ServerStatus {
        return ServerStatus(
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
        )
    }

    @GET
    @Path("/{requestId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRequestDetails(
        @PathParam("requestId") requestId: String
    ): RunningSandboxRuntimeRequestVO? {
        val runningRequest = sandboxRunnableRequestService.runningRequests.find { it.id == requestId } ?: return null
        return RunningSandboxRuntimeRequestVO(
            id = runningRequest.id,
            createTime = runningRequest.createTime,
            status = runningRequest.status,
            request = runningRequest.request
        )
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
