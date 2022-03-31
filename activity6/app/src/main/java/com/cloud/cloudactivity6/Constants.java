package com.cloud.cloudactivity6;

import com.amazonaws.auth.BasicAWSCredentials;

public interface Constants {

    String AWS_ACCESS_KEY = "AKIA5D5TWPSJXYRY3Y5J";
    String AWS_SECRET_KEY = "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";
    String BUCKET_NAME = "cloudactivity6bucket";
    BasicAWSCredentials creds = new BasicAWSCredentials(Constants.AWS_ACCESS_KEY, Constants.AWS_SECRET_KEY);
}
