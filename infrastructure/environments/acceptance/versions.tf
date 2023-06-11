terraform {

  backend "s3" {
    bucket = "sander-terraform-bucket"
    key    = "my-function-acceptance.tfstate"
    region = "eu-central-1"
  }
}
