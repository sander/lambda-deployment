//> using scala 3.2.1
//> using dep com.amazonaws:aws-lambda-java-core:1.2.2
//> using dep com.amazonaws:aws-lambda-java-events:3.11.2

package nl.sanderdijkhuis.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.{APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse};
import java.util.HashMap

class MyRequestHandler:
  def handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse =
    val logger = context.getLogger()
    logger.log("event: " + event)
    val responseMessage = "Hello, World"
    val response = APIGatewayV2HTTPResponse()
    response.setIsBase64Encoded(false)
    response.setStatusCode(200)
    val headers = HashMap[String, String]()
    headers.put("Content-Type", "application/json")
    response.setHeaders(headers)
    val body = s"{\"message\":\"$responseMessage\"}"
    response.setBody(body)
    response
