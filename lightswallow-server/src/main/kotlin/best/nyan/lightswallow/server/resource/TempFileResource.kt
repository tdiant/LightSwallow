package best.nyan.lightswallow.server.resource

import best.nyan.lightswallow.server.config.AppConfig
import org.jboss.resteasy.reactive.multipart.FileUpload
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import kotlin.io.path.Path

@Path("/temp-file")
class TempFileResource(
    private val appConfig: AppConfig
) {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    fun uploadStaticFile(
        @HeaderParam("secret-key") @DefaultValue("") secretKey: String,
        @FormParam("file") file: FileUpload
    ): String? {
        if (!appConfig.verifySecretKey(secretKey))
            return null

        val fileUUID = UUID.randomUUID().toString()
        Files.move(file.uploadedFile(), Path(appConfig.tempFilePath, fileUUID), StandardCopyOption.REPLACE_EXISTING)

        return fileUUID
    }

}
