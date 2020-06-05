package com.socialmediaraiser.twitterbot.impl.personalAnalyzer;

import com.socialmediaraiser.twitter.TwitterClient;
import com.socialmediaraiser.twitter.dto.user.IUser;
import com.socialmediaraiser.twitter.helpers.ConverterHelper;
import com.socialmediaraiser.twitterbot.AbstractIOHelper;
import com.socialmediaraiser.twitterbot.GoogleSheetHelper;
import com.socialmediaraiser.twitterbot.impl.User;
import io.vavr.collection.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;


@Getter
@Setter
@CustomLog
public class PersonalAnalyzerBot {

  private       String            userName;
  private       AbstractIOHelper  ioHelper;
  private       TwitterClient     twitterClient = new TwitterClient();
  private final Date              iniDate       = ConverterHelper.dayBeforeNow(30);
  private       DataArchiveHelper dataArchiveHelper;
  private       ApiSearchHelper   apiSearchHelper;

  public PersonalAnalyzerBot(String userName, String archiveFileName) {
    this.userName          = userName;
    this.ioHelper          = new GoogleSheetHelper(userName);
    this.dataArchiveHelper = new DataArchiveHelper(userName, archiveFileName, iniDate);
    this.apiSearchHelper   = new ApiSearchHelper(userName);
  }

  public void launch(boolean includeFollowers, boolean includeFollowings, boolean onyFollowBackFollowers, String ArchiveFileName)
  throws InterruptedException {
    String           userId       = this.twitterClient.getUserFromUserName(userName).getId();
    UserInteractions interactions = this.getNbInterractions(ArchiveFileName); // @todo to change
    List<IUser>      followings   = this.twitterClient.getFollowingUsers(userId);
    List<IUser>      followers    = this.twitterClient.getFollowerUsers(userId);
    Set<IUser> allUsers = new HashSet<>() { // @todo duplicate
      {
        addAll(followings);
        addAll(followers);
      }
    };

    List<User> usersToWrite = new ArrayList<>();
    int        nbUsersToAdd = 50;
    for (IUser iUser : allUsers) {
      if (hasToAddUser(iUser, followings, followers, includeFollowings, includeFollowers, onyFollowBackFollowers)) {
        User user = new User(iUser);
        user.setNbRepliesReceived(interactions.get(iUser.getId()).getNbRepliesReceived());
        user.setNbRepliesGiven(interactions.get(iUser.getId()).getNbRepliesGiven());
        user.setNbRetweetsReceived(interactions.get(iUser.getId()).getNbRetweetsReceived());
        user.setNbLikesGiven(interactions.get(iUser.getId()).getNbLikesGiven());
        user.setNbRetweetsGiven(interactions.get(iUser.getId()).getNbRetweetsGiven());
        usersToWrite.add(user);
        if (usersToWrite.size() == nbUsersToAdd) {
          this.ioHelper.addNewFollowerLineSimple(usersToWrite);
          usersToWrite = new ArrayList<>();
          LOGGER.info("adding " + nbUsersToAdd + " users ...");
          TimeUnit.MILLISECONDS.sleep(500);
        }
      }
    }
    this.ioHelper.addNewFollowerLineSimple(usersToWrite);
    LOGGER.info("finish with success");
  }

  private boolean hasToAddUser(IUser user, List<IUser> followings, List<IUser> followers,
                               boolean showFollowings, boolean showFollowers, boolean onyFollowBackUsers) {
    // case 0 : only follow back users
    if (onyFollowBackUsers && followings.contains(user) && !followers.contains(user)) {
      return false;
    }

    // case 1 : show all the people i'm following and all the users following me
    if (!showFollowers && !showFollowings) {
      return true;
    }
    // case 2 : show all the people I'm following who are following me back
    else if (showFollowers && showFollowings && onyFollowBackUsers) {
      return (followings.contains(user) && followers.contains(user));
    }
    // case 3 : show all the people i'm following or all the people who are following me
    else {
      return ((followings.contains(user) && showFollowings) || followers.contains(user) && showFollowers);
    }
  }

  private UserInteractions getNbInterractions(String archiveFileName) {
    UserInteractions              userInteractions     = new UserInteractions();
    Map<String, TweetInteraction> receivedInteractions = this.getReceivedInteractions();
    // counts all retweets given to others
    dataArchiveHelper.countRetweetsGiven(userInteractions);
    // counts all the unique replies given by the user to others
    dataArchiveHelper.countRepliesGiven(userInteractions);
    // counts all replies given recently to others
    apiSearchHelper.countRecentRepliesGiven(userInteractions,
                                            dataArchiveHelper.filterTweetsByRetweet(false).get(0).getCreatedAt()); // @todo test 2nd arg
    apiSearchHelper.countGivenLikesOnStatuses(userInteractions);
    return userInteractions;
  }

  private Map<String, TweetInteraction> getReceivedInteractions() {
    return dataArchiveHelper.countRetweetsReceived()
                          .merge(apiSearchHelper.countRepliesReceived(true),
                                 TweetInteraction::merge)
                          .merge(apiSearchHelper.countRepliesReceived(false)); // why not TweetInteraction::merge ?
  }

  @SneakyThrows
  public void unfollow(String[] toUnfollow, String[] whiteList) {

    int nbUnfollows = 0;
    for (String unfollowName : toUnfollow) {
      unfollowName.replace(" ", "");
      if (!Arrays.asList(whiteList).contains(unfollowName)) {
        this.getTwitterClient().unfollowByName(unfollowName);
        nbUnfollows++;
        TimeUnit.MILLISECONDS.sleep(500);
        System.out.println(unfollowName + " unfollowed");
      }
    }
    LOGGER.info(nbUnfollows + " users unfollowed with success !");
  }
}