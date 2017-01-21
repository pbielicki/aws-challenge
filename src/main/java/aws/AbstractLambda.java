package aws;

import static aws.Util.DEFAULT_REGION;
import static aws.Util.REGION;
import static aws.Util.getenv;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

public abstract class AbstractLambda {

	protected final Region region;
	protected final DynamoDB dynamoDB;

	public AbstractLambda() {
		region = Region.getRegion(Regions.fromName(getenv(REGION, DEFAULT_REGION)));
    AmazonDynamoDBClient dynamoClient = new AmazonDynamoDBClient();
    dynamoClient.setRegion(region);
    dynamoDB = new DynamoDB(dynamoClient);
  }
}
