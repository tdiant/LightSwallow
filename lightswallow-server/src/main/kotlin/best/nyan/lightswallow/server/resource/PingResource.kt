package best.nyan.lightswallow.server.resource

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/ping")
class PingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun ping() = "pong"

}
