package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequestResult
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class MessageQueueHandleResource(
    private val appConfig: AppConfig
) {

    @Incoming("sandbox-request")
    @Outgoing("sandbox-result")
    @Acknowledgment(value = Acknowledgment.Strategy.POST_PROCESSING)
    fun handleRequest(request: SandboxRunnableRequest): SandboxRunnableRequestResult {
        return request.runAndWait(
            chrootPath = appConfig.chrootPath,
            chdirRootPath = appConfig.chdirRootPath,
            homePath = appConfig.homePath,
            serverId = appConfig.serverId
        )
    }

}