package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequestResult
import best.nyan.lightswallow.server.service.SandboxRunnableRequestService
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import java.util.concurrent.CompletionStage
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider


@ApplicationScoped
class MessageQueueHandleResource(
    private val appConfig: AppConfig,
    private val sandboxRunnableRequestService: SandboxRunnableRequestService
) {

    @Incoming("sandbox-request")
    @Outgoing("sandbox-result")
    @Acknowledgment(value = Acknowledgment.Strategy.POST_PROCESSING)
    fun handleRequest(request: SandboxRunnableRequest): CompletionStage<SandboxRunnableRequestResult> {
        val runningRequest = sandboxRunnableRequestService.runRequest(request) ?: throw MessageQueueDenyException()
        return runningRequest.resultFuture.toCompletionStage()
    }

}

class MessageQueueDenyException : RuntimeException()

@Provider
class MessageQueueDenyExceptionMapper : ExceptionMapper<MessageQueueDenyException> {
    override fun toResponse(exception: MessageQueueDenyException): Response =
        Response.status(200).entity("").build()
}
