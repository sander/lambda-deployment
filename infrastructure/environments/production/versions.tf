terraform {

  backend "s3" {
    bucket = "sander-terraform-bucket"
    key    = "my-function-production.tfstate"
    region = "eu-central-1"
  }
}
