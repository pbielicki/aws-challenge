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

## Install EventScanner lambda and schedule its execution every 10 minutes

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
aws events put-rule --schedule-expression 'rate(10 minutes)' --name every_10_minutes
~~~

~~~~
aws lambda add-permission \
--function-name EventScanner \
--statement-id every_10_minutes_event \
--action 'lambda:InvokeFunction' \
--principal events.amazonaws.com \
--source-arn [scheduled-rule-arn]
~~~~

## Install NotificationSender lambda and schedule its execution every 10 minutes

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
--statement-id every_10_minutes_event \
--action 'lambda:InvokeFunction' \
--principal events.amazonaws.com \
--source-arn [scheduled-rule-arn]
~~~~
