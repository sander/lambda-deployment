import com.hashicorp.cdktf.*
import com.hashicorp.cdktf.providers.aws.apigatewayv2_api.Apigatewayv2Api
import com.hashicorp.cdktf.providers.aws.apigatewayv2_api.Apigatewayv2ApiConfig
import com.hashicorp.cdktf.providers.aws.apigatewayv2_integration.Apigatewayv2Integration
import com.hashicorp.cdktf.providers.aws.apigatewayv2_integration.Apigatewayv2IntegrationConfig
import com.hashicorp.cdktf.providers.aws.apigatewayv2_route.Apigatewayv2Route
import com.hashicorp.cdktf.providers.aws.apigatewayv2_route.Apigatewayv2RouteConfig
import com.hashicorp.cdktf.providers.aws.apigatewayv2_stage.Apigatewayv2Stage
import com.hashicorp.cdktf.providers.aws.apigatewayv2_stage.Apigatewayv2StageAccessLogSettings
import com.hashicorp.cdktf.providers.aws.apigatewayv2_stage.Apigatewayv2StageConfig
import com.hashicorp.cdktf.providers.aws.cloudwatch_log_group.CloudwatchLogGroup
import com.hashicorp.cdktf.providers.aws.cloudwatch_log_group.CloudwatchLogGroupConfig
import com.hashicorp.cdktf.providers.aws.iam_role.IamRole
import com.hashicorp.cdktf.providers.aws.iam_role.IamRoleConfig
import com.hashicorp.cdktf.providers.aws.iam_role_policy_attachment.IamRolePolicyAttachment
import com.hashicorp.cdktf.providers.aws.iam_role_policy_attachment.IamRolePolicyAttachmentConfig
import com.hashicorp.cdktf.providers.aws.lambda_function.LambdaFunction
import com.hashicorp.cdktf.providers.aws.lambda_function.LambdaFunctionConfig
import com.hashicorp.cdktf.providers.aws.lambda_permission.LambdaPermission
import com.hashicorp.cdktf.providers.aws.lambda_permission.LambdaPermissionConfig
import com.hashicorp.cdktf.providers.aws.provider.AwsProvider
import com.hashicorp.cdktf.providers.aws.provider.AwsProviderConfig
import com.hashicorp.cdktf.providers.aws.s3_bucket.S3Bucket
import com.hashicorp.cdktf.providers.aws.s3_bucket.S3BucketConfig
import com.hashicorp.cdktf.providers.aws.s3_bucket_acl.S3BucketAcl
import com.hashicorp.cdktf.providers.aws.s3_bucket_acl.S3BucketAclConfig
import com.hashicorp.cdktf.providers.aws.s3_bucket_ownership_controls.S3BucketOwnershipControls
import com.hashicorp.cdktf.providers.aws.s3_bucket_ownership_controls.S3BucketOwnershipControlsConfig
import com.hashicorp.cdktf.providers.aws.s3_bucket_ownership_controls.S3BucketOwnershipControlsRule
import com.hashicorp.cdktf.providers.aws.s3_object.S3Object
import com.hashicorp.cdktf.providers.aws.s3_object.S3ObjectConfig
import com.hashicorp.cdktf.providers.random_provider.pet.Pet
import com.hashicorp.cdktf.providers.random_provider.pet.PetConfig
import com.hashicorp.cdktf.providers.random_provider.provider.RandomProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import software.constructs.Construct
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

fun main(args: Array<String>) {
    val app = App()
    MainStack(app, "learn-cdktf-lambda")
    app.synth()
}

class MainStack(scope: Construct, id: String) : TerraformStack(scope, id) {
    init {
        AwsProvider(this, "aws", AwsProviderConfig.builder().region("eu-central-1").build())
        RandomProvider(this, "random")
        S3Backend(this, S3BackendConfig.builder().bucket("sander-terraform-bucket").key("cdktf/key").region("eu-central-1").build())

        val pet = Pet(this, "pet", PetConfig.builder().prefix("cdktf-lambda").length(2).build())

        val asset = TerraformAsset(this, "lambda-asset", TerraformAssetConfig.builder().path("../out/hello.jar").type(AssetType.FILE).build())
        println(asset.assetHash)
        val hash = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get("../out/hello.jar")))
        println(BigInteger(1, hash).toString(16))
        val bucket = S3Bucket(this, "lambda_bucket", S3BucketConfig.builder().bucket(pet.id).build())
        val ownership = S3BucketOwnershipControls(this, "s3_bucket_acl_ownership", S3BucketOwnershipControlsConfig.builder().bucket(bucket.id).rule(
            S3BucketOwnershipControlsRule.builder().objectOwnership("ObjectWriter").build()).build())
        val acl = S3BucketAcl(this, "bucket_acl", S3BucketAclConfig.builder().bucket(bucket.id).acl("private").dependsOn(listOf(ownership)).build())
        val obj = S3Object(this, "lambda_hello_world", S3ObjectConfig.builder().bucket(bucket.id).key("hello-world.jar").source(asset.path)/*.etag(asset.assetHash)*/.build())

        val policy = Json.encodeToString(
            buildJsonObject {
                put("Version", "2012-10-17")
                putJsonArray("Statement") {
                    addJsonObject {
                        put("Action", "sts:AssumeRole")
                        put("Effect", "Allow")
                        put("Sid", "")
                        putJsonObject("Principal") {
                            put("Service", "lambda.amazonaws.com")
                        }
                    }
                }
            }
        )
        println(policy)
        val role = IamRole(this, "lambda_exec", IamRoleConfig.builder()
            .name("serverless_lambda")
            .assumeRolePolicy(policy)
            .build())
        val function = LambdaFunction(this, "hello_world_function", LambdaFunctionConfig.builder()
            .functionName("HelloWorld")
            .s3Bucket(bucket.id)
            .s3Key(obj.key)
            .runtime("java17")
            .handler("nl.sanderdijkhuis.lambda.MyRequestHandler::handleRequest")
            .sourceCodeHash(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Paths.get("../out/hello.jar")))))
            .role(role.arn)
            .build())
        val logGroup = CloudwatchLogGroup(this, "hello_world_log", CloudwatchLogGroupConfig.builder()
            .name("/aws/lambda/${function.functionName}")
            .retentionInDays(30)
            .build())
        val policyAttachment = IamRolePolicyAttachment(this, "lambda_policy", IamRolePolicyAttachmentConfig.builder()
            .role(role.name)
            .policyArn("arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole")
            .build())

        TerraformOutput(this, "function_name", TerraformOutputConfig.builder().value(function.functionName).build())

        val api = Apigatewayv2Api(this, "lambda-api", Apigatewayv2ApiConfig.builder()
            .name("serverless_lambda_gw")
            .protocolType("HTTP")
            .build())
        val apiLogGroup = CloudwatchLogGroup(this, "hello_world_api_log", CloudwatchLogGroupConfig.builder()
            .name("/aws/api_gw/${api.name}")
            .retentionInDays(30)
            .build())
        val stage = Apigatewayv2Stage(this, "lambda-stage", Apigatewayv2StageConfig.builder()
            .apiId(api.id)
            .name("serverless_lambda_stage")
            .autoDeploy(true)
            .accessLogSettings(Apigatewayv2StageAccessLogSettings.builder()
                .destinationArn(apiLogGroup.arn)
                .format(Json.encodeToString(
                    buildJsonObject {
                        put("requestId", "\$context.requestId")
                        put("sourceIp", "\$context.identity.sourceIp")
                        put("requestTime", "\$context.requestTime")
                        put("protocol", "\$context.protocol")
                        put("httpMethod", "\$context.httpMethod")
                        put("resourcePath", "\$context.resourcePath")
                        put("routeKey", "\$context.routeKey")
                        put("status", "\$context.status")
                        put("responseLength", "\$context.responseLength")
                        put("integrationErrorMessage", "\$context.integrationErrorMessage")
                    }
                ))
                .build())
            .build())
        val integration = Apigatewayv2Integration(this, "hello_world", Apigatewayv2IntegrationConfig.builder()
            .apiId(api.id)
            .integrationUri(function.invokeArn)
            .integrationType("AWS_PROXY")
            .integrationMethod("POST")
            .build())
        val route = Apigatewayv2Route(this, "hello_world_route", Apigatewayv2RouteConfig.builder()
            .apiId(api.id)
            .routeKey("GET /hello")
            .target("integrations/${integration.id}")
            .build())
        val permission = LambdaPermission(this, "api_gw", LambdaPermissionConfig.builder()
            .statementId("AllowExecutionFromAPIGateway")
            .action("lambda:InvokeFunction")
            .functionName(function.functionName)
            .principal("apigateway.amazonaws.com")
            .sourceArn("${api.executionArn}/*/*")
            .build())
        TerraformOutput(this, "base_url", TerraformOutputConfig.builder().value(stage.invokeUrl).build())
    }
}