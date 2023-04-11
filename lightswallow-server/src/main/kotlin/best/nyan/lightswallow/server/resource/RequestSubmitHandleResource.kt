package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.service.SandboxRunnableRequestService
import org.jboss.resteasy.reactive.RestResponse
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/submit")
class RequestSubmitHandleResource(
    val appConfig: AppConfig,
    val service: SandboxRunnableRequestService
) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun handleRequest(
        @HeaderParam("secret-key") @DefaultValue("") secretKey: String,
        request: SandboxRunnableRequest
    ): RestResponse<StatusResource.RunningSandboxRuntimeRequestListVO> {
        if (!appConfig.verifySecretKey(secretKey))
            return RestResponse.status(403)

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
