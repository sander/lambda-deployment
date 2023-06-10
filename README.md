# Lambda deployment

Compile the function:

    mkdir -p out
    scala-cli --power package my-function/my-function.scala -o out/hello.jar --assembly --preamble=false

Deploy the infrastructure:

    cd infrastructure
    cdktf deploy learn-cdktf-lambda

Get the base URL from the output and call it, e.g.:

    curl https://dmqmopuw42.execute-api.eu-central-1.amazonaws.com/serverless_lambda_stage/hello
