package uk.nhs.england.fwoa

import com.amazonaws.serverless.exceptions.ContainerInitializationException
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler
import com.amazonaws.serverless.proxy.model.AwsProxyRequest
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class FHIRRestHandler : RequestStreamHandler {
    private var handler: SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> = getHandler()

    fun getHandler() : SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>
    {

        handler = try {
            SpringBootLambdaContainerHandler.getAwsProxyHandler(FhirValidatorApplication::class.java);
        } catch (e : ContainerInitializationException) {
            // if we fail here. We re-throw the exception to force another cold start
            e.printStackTrace();
            throw RuntimeException ("Could not initialize Spring Boot application", e);
        }
        return handler
    }

    @Throws(IOException::class)
    override fun handleRequest(inputStream: InputStream?, outputStream: OutputStream?, context: Context?) {
       /* handler.stripBasePath("/validation/FHIR/R4")
        val request = LambdaContainerHandler.getObjectMapper().readValue(inputStream, AwsProxyRequest::class.java)
        if (context != null) {
            context.logger.log("Path before: " + request.path)
        };
        if (inputStream != null) {
            inputStream.reset()
        }

        */
        handler.proxyStream(inputStream, outputStream, context)
        /*
        if (context != null) {
            context.logger.log("Path after: " + request.path)
        };*/
    }
}