### Technical constraints

* No Unit Tests! well - no time
* No check for valid Sns message
* No throttling of messages per user - can cause OOM
* No message size verification - can cause OOM
* No verification for duplicate messages - can easily cause OOM
* No message sorting by timestamp (is it needed?)
* Working in AWS sandbox thus not able to send email to any recipient - only to verified ones
* Sending email notification in scheduled mode - not immediately after collecting the digest
* Retrying email send after 5 minutes infinitely or until next digest overwrites the old one
* No email notification max retry / retention / throttling - can blow up DynamoDB with millions of users
* Your observation goes here... :)

## Clone and build project

~~~~
git clone https://github.com/pbielicki/aws-challenge.git
cd aws-challenge
mvn package
~~~~

You should see:

~~~~
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
~~~~

All the other actions MUST be executed from within `aws-challenge/` directory.

## Prerequisites
Specific role for running AWS Lambda must be created in IAM console. This role
must have the following policy: `AmazonDynamoDBFullAccess`.

## Create DynamoDB tables

~~~~
aws dynamodb create-table \
--table-name UserEvent \
--attribute-definitions AttributeName=email,AttributeType=S \
--key-schema AttributeName=email,KeyType=HASH \
--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
~~~~

~~~~
aws dynamodb create-table \
--table-name UserNotification \
--attribute-definitions AttributeName=email,AttributeType=S \
--key-schema AttributeName=email,KeyType=HASH \
--provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
~~~~

## Install EventHandler lambda and subscribe to SNS topic

~~~~
aws lambda create-function \
--region eu-west-1 \
--function-name EventHandler \
--zip-file fileb://target/lambda-aws-challenge-1.0-SNAPSHOT.jar \
--role [role] \
--handler aws.EventHandlerLambda::handleEvent \
--runtime java8 \
--memory-size 512 \
--timeout 15
~~~~

~~~~
aws sns subscribe \
--topic arn:aws:sns:eu-west-1:963797398573:challenge-notifications \
--protocol lambda \
--notification-endpoint [EventHandler-arn]
~~~~

## Install EventScanner lambda and schedule its execution every 5 minutes

~~~~
aws lambda create-function \
--region eu-west-1 \
--function-name EventScanner \
--zip-file fileb://target/lambda-aws-challenge-1.0-SNAPSHOT.jar \
--role [role] \
--handler aws.EventScannerLambda::handleEvent \
--runtime java8 \
--memory-size 512 \
--timeout 15
~~~~

~~~~
aws events put-rule --schedule-expression 'rate(5 minutes)' --name every_5_minutes
~~~~

~~~~
aws lambda add-permission \
--function-name EventScanner \
--statement-id every_5_minutes_scanner \
--action 'lambda:InvokeFunction' \
--principal events.amazonaws.com \
--source-arn [scheduled-rule-arn]
~~~~

## Install NotificationSender lambda and schedule its execution every 5 minutes

~~~~
aws lambda create-function \
--region eu-west-1 \
--function-name NotificationSender \
--zip-file fileb://target/lambda-aws-challenge-1.0-SNAPSHOT.jar \
--role [role] \
--handler aws.NotificationSenderLambda::handleEvent \
--runtime java8 \
--memory-size 512 \
--timeout 15
~~~~

~~~~
aws lambda add-permission \
--function-name NotificationSender \
--statement-id every_5_minutes_sender \
--action 'lambda:InvokeFunction' \
--principal events.amazonaws.com \
--source-arn [scheduled-rule-arn]
~~~~

I noticed that scheduling Lambda from CLI does not really work. The trigger is displayed in the console UI
but Lambda is not receiving scheduled events.

The workaround for this is to add `CloudWatch Events - Schedule` from the UI.

## Apply Identity Policies for each "To" email address that will be used (if AWS sandboxed account)

In sandbox AWS account, you not only have to validate each "From" email address but also you need
to allow your code (Lambda in our case) to send email "To" specific email addresses by
applying relevant Identity Policies in [SES management console](https://console.aws.amazon.com/ses).

Example of policy:

~~~~
{
  "Version": "2008-10-17",
  "Statement": [
    {
      "Sid": "stmt1485113930894",
      "Effect": "Allow",
      "Principal": {
         "AWS": "arn:aws:sts::[account-no]:[lambda-runner-role]/NotificationSender"
      },
      "Action": "ses:SendEmail",
      "Resource": "arn:aws:ses:eu-west-1:817181745424:identity/example@email.com"
    }
  ]
}
~~~~

