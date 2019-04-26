package com.twitter.helpers;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.twitter.config.SignatureConstants;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.json.JSONObject;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
public class RequestHelper {

    public JSONObject executeRequest(String url, RequestMethod method) {
        try {
            switch (method) {
                case GET:
                    return executeGetRequest(url);
                case POST:
                    return executePostRequest(url);
                default:
                    return null;
            }
        } catch(Exception e){
            return null;
        }
    }


    private JSONObject executeGetRequest(String url) {

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try {
            OkHttpClient client = new OkHttpClient();
            client.setConnectTimeout(60, TimeUnit.SECONDS); // connect timeout
            client.setReadTimeout(60, TimeUnit.SECONDS);    // socket timeout
            Response response = client.newCall(this.getSignedRequest(request, this.getNonce(), this.getTimestamp())).execute();
            LocalDateTime now = LocalDateTime.now();
         //   System.out.println(now.getHour() + ":" + now.getMinute() + " executing request on " + url);
            JSONObject jsonResponse = new JSONObject(response.body().string());
            if(response.code()==200){
                return jsonResponse;
            } else if (response.code()==429){
                System.out.println(response.message() +" at "
                        + now.getHour() + ":" + now.getMinute() + ". Waiting ..."); // do a wait and return this function recursively
                try {
                    TimeUnit.MINUTES.sleep(4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return this.executeGetRequest(url);
            } else{
                return null;
            }
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject executePostRequest(String url) {

        RequestBody reqbody = RequestBody.create(null, new byte[0]);

        Request request = new Request.Builder()
                .url(url)
                .post(reqbody)
                .build();

        try {
            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(this.getSignedRequest(request, this.getNonce(), this.getTimestamp())).execute();
            LocalDateTime now = LocalDateTime.now();
      //      System.out.println(now.getHour() + ":" + now.getMinute() + " executing request on " + url);
            return new JSONObject(response.body().string());

        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private String getNonce(){
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            stringBuilder.append(secureRandom.nextInt(10));
        }
        return stringBuilder.toString();
    }

    private String getTimestamp(){
        return String.valueOf(new Timestamp(System.currentTimeMillis()).getTime()).substring(0,10);
    }

    private Request getSignedRequest(Request request, String nonce, String timestamp) throws IOException {
        Oauth1SigningInterceptor oauth = new Oauth1SigningInterceptor.Builder()
                .consumerKey(SignatureConstants.consumerKey)
                .consumerSecret(SignatureConstants.consumerSecret)
                .accessToken(SignatureConstants.accessToken)
                .accessSecret(SignatureConstants.secretToken)
                .oauthNonce(nonce)
                .oauthTimeStamp(timestamp)
                .build();

        return oauth.signRequest(request);
    }
}
