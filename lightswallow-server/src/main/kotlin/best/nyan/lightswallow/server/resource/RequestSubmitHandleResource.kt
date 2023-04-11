package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.service.SandboxRunnableRequestService
import org.jboss.resteasy.reactive.RestResponse
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/submit")
class RequestSubmitHandleResource(
    val service: SandboxRunnableRequestService
) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleRequest(request: SandboxRunnableRequest): RestResponse<StatusResource.RunningSandboxRuntimeRequestListVO> {
        val runningRequest = service.runRequest(request) ?: return RestResponse.serverError()
        return RestResponse.ok(runningRequest.let {
            StatusResource.RunningSandboxRuntimeRequestListVO(
                id = it.id,
                createTime = it.createTime,
                status = it.status
            )
        })
    }

}
