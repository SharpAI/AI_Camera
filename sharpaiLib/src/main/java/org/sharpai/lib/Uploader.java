package org.sharpai.lib;

import android.util.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.PutObjectResult;

import java.io.File;

public class Uploader {
    AmazonS3Client mClient = null;
    //private String mEndpoint = "http://165.232.62.29:9000";
    //private String mEndpoint = "http://192.168.1.5:9000";
    private String mEndpoint = "http://10.168.1.221:9000";
    private String mBucket = "faces";
    private String mUrlPrefix= mEndpoint + "/" +mBucket;
    private boolean mIfcheckedBucket = false;
    private String AWS_PUBLIC_READ_POLICY_TEXT = "{\n" +
            " \"Statement\": [\n" +
            "  {\n" +
            "   \"Action\": [\n" +
            "    \"s3:GetObject\"\n" +
            "   ],\n" +
            "   \"Effect\": \"Allow\",\n" +
            "   \"Principal\": {\n" +
            "    \"AWS\": [\n" +
            "     \"*\"\n" +
            "    ]\n" +
            "   },\n" +
            "   \"Resource\": [\n" +
            "    \"arn:aws:s3:::faces/*\"\n" +
            "   ]\n" +
            "  }\n" +
            " ],\n" +
            " \"Version\": \"2012-10-17\"\n" +
            "}";
    private String getFileUrl(String fileName){
        return mUrlPrefix+"/"+fileName;
    }

    public Uploader(String savedMinioIP, String savedMinioPort){
        AWSCredentials credentials = new BasicAWSCredentials("QUA2IU17RHOKE6NUZ7T2", "MQniMc5K3lsRbv9OPaUbJN9ft2eTQJ1rh4Yx6C17");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");
        mEndpoint = "http://"+savedMinioIP+":"+savedMinioPort;
        mUrlPrefix= mEndpoint + "/" +mBucket;

        mClient = new AmazonS3Client(credentials, clientConfiguration);
        mClient.setEndpoint(mEndpoint);

        S3ClientOptions options = new S3ClientOptions();
        options.setPathStyleAccess(true);
        mClient.setS3ClientOptions(options);
    }
    public String putObject(String fileName,String filePath){
        try{

            if(mIfcheckedBucket == false){
                if (mClient.doesBucketExist(mBucket)) {
                    Log.d("UPLOADER","Bucket "+ mBucket +" already exists.");
                } else {
                    try {
                        mClient.createBucket(mBucket);
                        mClient.setBucketPolicy(mBucket, AWS_PUBLIC_READ_POLICY_TEXT);
                    } catch (AmazonS3Exception e) {
                        e.printStackTrace();
                    }
                }
                mIfcheckedBucket = true;
            }
            PutObjectResult result = mClient.putObject("faces", fileName, new File(filePath));

            Log.d("UPLOADER","file uploaded: "+getFileUrl(fileName));
            return getFileUrl(fileName);
        } catch (com.amazonaws.AmazonClientException e){
            e.printStackTrace();
            return "";
        }
    }
}
