package com.socialmediaraiser.twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.socialmediaraiser.AbstractBot;
import com.socialmediaraiser.RelationType;
import com.socialmediaraiser.twitter.helpers.GoogleSheetHelper;
import com.socialmediaraiser.twitter.helpers.JsonHelper;
import com.socialmediaraiser.twitter.helpers.RequestHelper;
import com.socialmediaraiser.twitter.helpers.URLHelper;
import com.socialmediaraiser.twitter.helpers.dto.getrelationship.RelationshipDTO;
import com.socialmediaraiser.twitter.helpers.dto.getrelationship.RelationshipObjectResponseDTO;
import com.socialmediaraiser.twitter.helpers.dto.getuser.AbstractUser;
import com.socialmediaraiser.twitter.scoring.Criterion;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import lombok.Data;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Data
public abstract class AbstractTwitterBot extends AbstractBot implements ITwitterBot{

    private static final Logger LOGGER = Logger.getLogger(AbstractTwitterBot.class.getName());
    private String ownerName;
    private List<AbstractUser> potentialFollowers = new ArrayList<>();
    List<String> followedRecently = this.getIoHelper().getPreviouslyFollowedIds();
    List<String> ownerFollowingIds;
    private boolean follow; // if try will follow users
    private boolean saveResults;
    private URLHelper urlHelper = new URLHelper();
    private RequestHelper requestHelper = new RequestHelper();
    private JsonHelper jsonHelper = new JsonHelper();
    private static final String IDS = "ids";
    private static final String USERS = "users";
    private static final String CURSOR = "cursor";
    private static final String NEXT = "next";
    private static final String RETWEET_COUNT = "retweet_count";
    private static final String RELATIONSHIP = "relationship";
    private static final String FOLLOWING = "following";
    private static final String FOLLOWED_BY = "followed_by";
    private static final String SOURCE = "source";
 //   private static final int MAX_GET_F_CALLS = 30;

    public AbstractTwitterBot(String ownerName, boolean follow, boolean saveResults){
        super(new GoogleSheetHelper(ownerName));
        this.ownerName = ownerName;
        this.follow = follow;
        this.saveResults = saveResults;
        this.ownerFollowingIds = this.getFollowingIds(this.getUserFromUserName(ownerName).getId());
    }


    // can manage up to 5000 results / call . Max 15 calls / 15min ==> 75.000 results max. / 15min
    private List<String> getUserIdsByRelation(String url){
        Long cursor = -1L;
        List<String> result = new ArrayList<>();
        int nbCalls = 1;
        do {
            String urlWithCursor = url + "&"+CURSOR+"=" + cursor;
            JsonNode response = this.getRequestHelper().executeGetRequest(urlWithCursor);
            if(response!=null && response.has(IDS)){
                List<String> ids = this.getJsonHelper().jsonLongArrayToList(response);
                if(ids!=null){
                    result.addAll(ids);
                }
            } else{
                LOGGER.severe(()->"response null or ids not found !");
                return result;
            }

            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
        }
        while (cursor != null && cursor != 0 /*&& nbCalls < MAX_GET_F_CALLS*/);
        return result;
    }

    private Set<String> getUserIdsByRelationSet(String url){
        Long cursor = -1L;
        Set<String> result = new HashSet<>();
        int nbCalls = 1;
        do {
            String urlWithCursor = url + "&"+CURSOR+"=" + cursor;
            JsonNode response = this.getRequestHelper().executeGetRequest(urlWithCursor);
            if(response!=null && response.has(IDS)){
                List<String> ids = this.getJsonHelper().jsonLongArrayToList(response);
                if(ids!=null){
                    result.addAll(ids);
                }
            } else{
                LOGGER.severe(()->"response null or ids not found !");
                return result;
            }

            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
        }
        while (cursor != null && cursor != 0 /*&& nbCalls < MAX_GET_F_CALLS*/);
        return result;
    }

    // can manage up to 200 results/call . Max 15 calls/15min ==> 3.000 results max./15min
    private List<AbstractUser> getUsersInfoByRelation(String url) {
        Long cursor = -1L;
        List<AbstractUser> result = new ArrayList<>();
        int nbCalls = 1;
        LOGGER.fine(() -> "users : ");
        do {
            String urlWithCursor = url + "&"+CURSOR+"=" + cursor;
            JsonNode response = this.getRequestHelper().executeGetRequest(urlWithCursor);
            if(response==null){
                break;
            }
            List<AbstractUser> users = this.getJsonHelper().jsonUserArrayToList(response.get(USERS));
            result.addAll(users);
            cursor = this.getJsonHelper().getLongFromCursorObject(response);
            nbCalls++;
            LOGGER.info(result.size() + " | ");
        } while (cursor != 0 && cursor!=null /*&& nbCalls < MAX_GET_F_CALLS*/);
        LOGGER.info("\n");
        return result;
    }

    private List<String> getUserIdsByRelation(String userId, RelationType relationType){
        String url = null;
        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerIdsUrl(userId);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingIdsUrl(userId);
        }
        return this.getUserIdsByRelation(url);
    }

    public Set<String> getUserFollowersIds(String userId){
        return this.getUserIdsByRelationSet(this.urlHelper.getFollowerIdsUrl(userId));
    }


    private List<AbstractUser> getUsersInfoByRelation(String userId, RelationType relationType) {
        String url = null;
        if(relationType == RelationType.FOLLOWER){
            url = this.urlHelper.getFollowerUsersUrl(userId);
        } else if (relationType == RelationType.FOLLOWING){
            url = this.urlHelper.getFollowingUsersUrl(userId);
        }
        return this.getUsersInfoByRelation(url);
    }

    @Override
    public List<String> getFollowerIds(String userId)  {
        return this.getUserIdsByRelation(userId, RelationType.FOLLOWER);
    }

    @Override
    public List<AbstractUser> getFollowerUsers(String userId) {
        return this.getUsersInfoByRelation(userId, RelationType.FOLLOWER);
    }

    @Override
    public List<String> getFollowingIds(String userId) {
        return this.getUserIdsByRelation(userId, RelationType.FOLLOWING);
    }

    @Override
    public List<AbstractUser> getFollowingsUsers(String userId) {
        return this.getUsersInfoByRelation(userId, RelationType.FOLLOWING);
    }

    @Override
    public RelationType getRelationType(String userId1, String userId2){
        String url = this.urlHelper.getFriendshipUrl(userId1, userId2);
        String response = this.getRequestHelper().executeGetRequestV2(url);
        if(response!=null) {
            try {
                RelationshipDTO relationshipDTO = JsonHelper.OBJECT_MAPPER.readValue(this.getRequestHelper().executeGetRequestV2(url), RelationshipObjectResponseDTO.class).getRelationship();
                Boolean followedBy = relationshipDTO.getSource().isFollowedBy();
                Boolean following = relationshipDTO.getSource().isFollowing();
                if (followedBy && following){
                    return RelationType.FRIENDS;
                } else if (!followedBy && !following){
                    return RelationType.NONE;
                } else if(followedBy){
                    return RelationType.FOLLOWER;
                } else{
                    return RelationType.FOLLOWING;
                }

            } catch (IOException e) {
                this.logError(e, response);
            }
        }
        LOGGER.severe(() -> "areFriends was null for " + userId2 + "! -> false ");
        return null;
    }

    // API KO
    @Override
    public List<String> getRetweetersId(String tweetId) {
        String url = this.urlHelper.getRetweetersUrl(tweetId);
        return this.getUserIdsByRelation(url);
    }

    @Override
    public boolean follow(String userId) {
        String url = this.urlHelper.getFollowUrl(userId);
        JsonNode jsonResponse = this.requestHelper.executePostRequest(url, new HashMap<>());
        if(jsonResponse!=null) {
            if (jsonResponse.has(JsonHelper.FOLLOWING)) {
                return true;
            } else{
                LOGGER.severe(()->"following property not found :(  " + userId + " not followed !");
            }
        }
        LOGGER.severe(()->"jsonResponse was null for user  " + userId);
        return false;
    }

    @Override
    public boolean unfollow(String userId) {
        String url = this.urlHelper.getUnfollowUrl(userId);
        JsonNode jsonResponse = this.requestHelper.executePostRequest(url, new HashMap<>());
        if(jsonResponse!=null){
            LOGGER.info(()->userId + " unfollowed");
            return true;
        }
        LOGGER.severe(()->userId + " not unfollowed");
        return false;
    }

    public AbstractUser getUserFromUserId(String userId)  {
        String url = this.getUrlHelper().getUserUrl(userId);
        String response = this.getRequestHelper().executeGetRequestV2(url);
        if(response!=null){
            try{
                return this.getJsonHelper().jsonResponseToUserV2(response);
            } catch(Exception e){
                this.logError(e, response);
            }
        }
        LOGGER.severe(()->"getUserFromUserId return null for " + userId);
        return null;
    }

    @Override
    public AbstractUser getUserFromUserName(String userName) {
        String url = this.getUrlHelper().getUserUrlFromName(userName);
        String response = this.getRequestHelper().executeGetRequestV2(url);
        if (response != null) {
            try {
                return this.getJsonHelper().jsonResponseToUserV2(response);
            } catch (IOException e) {
                this.logError(e, response);
            }
        }
        return null;
    }

    public List<AbstractUser> getUsersFromUserNames(List<String> userNames)  {
        String url = this.getUrlHelper().getUsersUrlbyNames(userNames);
        JsonNode response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null){
            return this.getJsonHelper().jsonUserArrayToList(response);
        } else{
            return new ArrayList<>();
        }
    }

    public List<AbstractUser> getUsersFromUserIds(List<String> userIds)  {
        String url = this.getUrlHelper().getUsersUrlbyIds(userIds);
        JsonNode response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null){
            return this.getJsonHelper().jsonUserArrayToList(response);
        } else{
            return new ArrayList<>();
        }
    }

    // @TODO to implement
    public String getRateLimitStatus(){
        String url = this.getUrlHelper().getRateLimitUrl();
        return this.getRequestHelper().executeGetRequestV2(url);
    }

    public void checkNotFollowBack(String ownerName, Date date, boolean override) {
        List<String> followedPreviously = this.getIoHelper().getPreviouslyFollowedIds(override, override, date);
        if(followedPreviously!=null && !followedPreviously.isEmpty()){
            AbstractUser user = this.getUserFromUserName(ownerName);
            this.areFriends(user.getId(), followedPreviously, this.isFollow(), this.isSaveResults());
        } else{
            LOGGER.severe(()->"no followers found at this date");
        }
    }

    public List<Tweet> getUserLastTweets(String userId, int count){
        String url = this.getUrlHelper().getUserTweetsUrl(userId, count);
        JsonNode response = this.getRequestHelper().executeGetRequestReturningArray(url);
        if(response!=null && response.size()>0){
            return this.getJsonHelper().jsonResponseToTweetList(response);
        }
        return new ArrayList<>();
    }

    public static List<Tweet> getUserLastTweetsStatic(String userId, int count){
        URLHelper urlHelper = new URLHelper();
        RequestHelper requestHelper = new RequestHelper();
        JsonHelper jsonHelper = new JsonHelper();
        String url = urlHelper.getUserTweetsUrl(userId, count);
        JsonNode response = requestHelper.executeGetRequestReturningArray(url); // @todo understand why sometime not X responses received as count requested
        if(response!=null && response.size()>0){
            return jsonHelper.jsonResponseToTweetList(response);
        }
        return new ArrayList<>();
    }

    @Override
    public void likeTweet(String tweetId) {
        String url = this.getUrlHelper().getLikeUrl(tweetId);
        this.getRequestHelper().executePostRequest(url, null);
    }

    @Override
    public void retweetTweet(String tweetId) {

    }

    public AbstractUser followNewUser(AbstractUser potentialFollower){
        boolean result = this.follow(potentialFollower.getId());
        if (result) {
            potentialFollower.setDateOfFollowNow();
            if (this.saveResults) {
                this.getIoHelper().addNewFollowerLine(potentialFollower);
            }
            return potentialFollower;
        }
        return null;
    }

    public List<Tweet> searchForTweetAnswers(String tweetId, String userName, String fromDate, String toDate){

        List<Tweet> all = this.searchForTweets("@"+userName, 10000, fromDate, toDate);
        List<Tweet> result = new ArrayList<>();
        for(Tweet tweet : all){
            if(tweet.getId().equals(tweetId)){
                result.add(tweet);
            }
        }
        return result;
    }
    // @todo remove count
    // date with yyyyMMddHHmm format
    @Override
    public List<Tweet> searchForTweets(String query, int count, String fromDate, String toDate){

        if(count<10){
            count = 10;
            LOGGER.severe(()->"count minimum = 10");
        }
        if(count>100){
            count = 100;
            LOGGER.severe(()->"count maximum = 100");
        }
        String url = this.getUrlHelper().getSearchTweetsUrl();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query",query);
        parameters.put("maxResults",String.valueOf(count));
        parameters.put("fromDate",fromDate);
        parameters.put("toDate",toDate);

        String next;
        List<Tweet> result = new ArrayList<>();
        int nbCalls = 1;
        do {
            JsonNode response = this.getRequestHelper().executePostRequest(url,parameters);
            JsonNode responseArray = null;
            try {
                responseArray = JsonHelper.OBJECT_MAPPER.readTree(response.get("results").toString());
            } catch (IOException e) {
                LOGGER.severe(e.getMessage());
            }

            if(response!=null && response.size()>0){
                result.addAll(this.getJsonHelper().jsonResponseToTweetListV2(responseArray));
            } else{
                LOGGER.severe(()->"response null or ids not found !");
            }

            if(!response.has(NEXT)){
                break;
            }
            next = response.get(NEXT).toString();
            parameters.put(NEXT, next);
            nbCalls++;
        }
        while (next!= null /*&& nbCalls < MAX_GET_F_CALLS*/ && result.size()<count);
        return result;
    }

    protected Authentication getAuthentication(){
        return new OAuth1(
                FollowProperties.getTwitterCredentials().getConsumerKey(),
                FollowProperties.getTwitterCredentials().getConsumerSecret(),
                FollowProperties.getTwitterCredentials().getAccessToken(),
                FollowProperties.getTwitterCredentials().getSecretToken());
    }

    private void logError(Exception e, String response){
        LOGGER.severe(() -> e.getMessage() + " response = " + response);
    }

    public void unfollowAllUsersFromCriterion(Criterion criterion, int value, boolean unfollow){
        int maxUnfollows = 400;
        int nbUnfollows = 0;
        for(String id : this.ownerFollowingIds){
            AbstractUser user = this.getUserFromUserId(id);
            if(nbUnfollows>=maxUnfollows) break;
            if(unfollow && user.shouldBeUnfollowed(criterion, value)){
                boolean result = this.unfollow(user.getId());
                nbUnfollows++;
                LOGGER.info(()-> user.getUsername() + " -> unfollowed");
                if(!result){
                    LOGGER.severe(() -> "error unfollowing " + user.getUsername());
                }
            }
        }
        LOGGER.info(nbUnfollows + " users unfollowed");
    }

    public boolean shouldFollow(AbstractUser user){
        return (ownerFollowingIds.indexOf(user.getId())==-1
                && followedRecently.indexOf(user.getId())==-1
                && potentialFollowers.indexOf(user)==-1
                && user.shouldBeFollowed(this.getOwnerName()));
    }
}
