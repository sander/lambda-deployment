# Lambda deployment

Compile the function:

    mkdir -p out
    scala-cli --power package my-function/my-function.scala -o out/hello.jar --assembly --preamble=false

Initialize Terraform:

    cd infrastructure/environments/production
    terraform init

Deploy the infrastructure:

    terraform apply

Get the base URL from the output:

    terraform output -raw base_url

Call it, e.g.:

    curl https://dmqmopuw42.execute-api.eu-central-1.amazonaws.com/serverless_lambda_stage/hello
