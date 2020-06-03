package com.socialmediaraiser.twitterbot.impl.followingBot;

import com.socialmediaraiser.twitter.dto.user.IUser;
import com.socialmediaraiser.twitter.dto.tweet.ITweet;
import com.socialmediaraiser.twitter.helpers.ConverterHelper;
import com.socialmediaraiser.twitterbot.AbstractTwitterFollowBot;
import lombok.Getter;
import lombok.Setter;
import lombok.CustomLog;

import java.util.Date;
import java.util.List;


@Getter
@Setter
@CustomLog
public class TwitterBotByLastActivity extends AbstractTwitterFollowBot {

    private int maxFriendship = 390;
    public TwitterBotByLastActivity(String ownerName, boolean follow, boolean saveResults) {
        super(ownerName, follow, saveResults);
    }

    @Override
    public List<IUser> getPotentialFollowers(String ownerId, int count){
        if(count>maxFriendship){
            count = maxFriendship;
        }

        List<ITweet> lastTweets = null;
        Date startDate = ConverterHelper.getDateFromString( "201907200000");
        Date endDate = ConverterHelper.getDateFromString("201907221000");
        lastTweets = this.getTwitterClient().searchForTweetsWithin30days("@"+this.getOwnerName(), startDate, endDate);
        int iteration=0;
        while(iteration<lastTweets.size() && this.getPotentialFollowers().size() < count){
            ITweet tweet = lastTweets.get(iteration);
            IUser potentialFollower = tweet.getUser();
            // @todo how to not count commonFollowers in scoring ?
            if(this.shouldFollow(potentialFollower)) {
                if (this.isFollow()) {
                    IUser user = this.followNewUser(potentialFollower);
                    if(user!=null) this.getPotentialFollowers().add(user);
                } else {
                    LOGGER.info(()->"potentialFollowers added : " + potentialFollower.getName());
                    this.getPotentialFollowers().add(potentialFollower);
                }
            }
            iteration++;
        }


        LOGGER.info(()->"********************************");
        LOGGER.info(this.getPotentialFollowers().size() + " followers followed / "
                + iteration + " users analyzed");
        LOGGER.info(()->"********************************");

        return this.getPotentialFollowers();
    }
}
