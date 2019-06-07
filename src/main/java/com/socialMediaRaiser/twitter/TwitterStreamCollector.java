package com.socialMediaRaiser.twitter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.socialMediaRaiser.twitter.signature.SignatureConstants;
import com.socialMediaRaiser.twitter.impl.TwitterBotByInfluencers;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TwitterStreamCollector {
    private int QUEUE_SIZE = 100;
    private TwitterBotByInfluencers bot = new TwitterBotByInfluencers();

    List<Long> ownerFollowers;

    public void collect() throws IOException, InterruptedException {

        final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
        final StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

        // add some track terms with this code:
        endpoint.trackTerms(Lists.newArrayList("decathlon"));
     //   List<Long> followings = new ArrayList();
     //   followings.add(92073489L);
     //   endpoint.followings(followings);

        // Create a new BasicClient. By default gzip is enabled.
        final Client client = new ClientBuilder()
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(this.getAuthentication())
                .processor(new StringDelimitedProcessor(queue))
                .build();

        // Establish a connection
        client.connect();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

       // this.ownerFollowers = bot.getFollowerIds(92073489L);

        while (!client.isDone()) {
            if(queue.size()>0){
                this.doAction(objectMapper.readValue(queue.take(), Tweet.class));
            }
        }
        client.stop();
    }

    public void doAction(Tweet tweet){
        System.out.println(tweet);

        bot.likeTweet(tweet.getId());

        User user = tweet.getUser();
        if(user.shouldBeFollowed()){
            user.addLanguageFromLastTweet(bot.getUserLastTweets(user.getId(), 2));
            if(user.getLang()!=null && user.getLang().equals(FollowProperties.getStringProperty(FollowProperties.LANGUAGE))){
                bot.follow(user.getId());
                bot.getIOHelper().addNewFollowerLine(user);
            }
        }
    }

    private Authentication getAuthentication(){
        // Authentication
        ObjectMapper mapper = new ObjectMapper();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File from = new File(classLoader.getResource(FollowProperties.TWEET_NAME+"/twitter_credentials.json").getFile());
        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<>() {};

        HashMap<String,Object> o = null;
        try {
            o = mapper.readValue(from, typeRef);
            return new OAuth1(
                    o.get(SignatureConstants.CONSUMER_KEY).toString(),
                    o.get(SignatureConstants.CONSUMER_SECRET).toString(),
                    o.get(SignatureConstants.ACCESS_TOKEN).toString(),
                    o.get(SignatureConstants.SECRET_TOKEN).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}