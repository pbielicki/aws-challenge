package aws;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "aws.messages";
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	public static final String EMAIL_MSG_HI = "email.msg.hi";
	public static final String EMAIL_MSG_LINE = "email.msg.line";
	public static final String EMAIL_SUBJECT = "email.subject";

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
