package com.github.redouane59.twitterbot.impl;

import com.github.redouane59.twitter.dto.tweet.ITweet;
import com.github.redouane59.twitter.dto.tweet.TweetType;
import com.github.redouane59.twitter.dto.user.IUser;
import com.github.redouane59.twitter.helpers.ConverterHelper;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.apache.commons.lang3.time.DateUtils;

@CustomLog
public class ApiSearchHelper extends AbstractSearchHelper {

  public ApiSearchHelper(String userName) {
    super(userName);
  }

  public Map<String, TweetInteraction> countRecentRetweetsReceived(Date mostRecentArchiveTweetDate){
    LOGGER.info("\nCounting recent retweets received (API)...");
    String       query = "retweets_of:" + this.getUserName();
    Tuple2<Date, Date> dates = getDatesFromMostRecentTweetDate(mostRecentArchiveTweetDate);
    if(dates==null) return HashMap.empty();
    Stream<ITweet> givenReplies = Stream.ofAll(this.getTwitterClient().searchForTweetsWithin7days(query, dates._1(), dates._2()));
    return givenReplies
        .filter(tweet -> tweet.getInReplyToUserId()==null)
        .filter(tweet -> this.isUserInList(tweet.getAuthorId()))
        .peek(tweet -> LOGGER.info("analyzing API recent reply : " + tweet.getText()))
        .groupBy(ITweet::getAuthorId)
        .map(this::getTupleRetweetReceived);
  }

  public Map<String, UserInteraction> countRecentRepliesGiven(Date mostRecentArchiveTweetDate) {
    LOGGER.info("\nCounting recent replies given (API)...");
    String       query = "(from:" + this.getUserName() + " has:mentions)";
    Tuple2<Date, Date> dates = getDatesFromMostRecentTweetDate(mostRecentArchiveTweetDate);
    if(dates==null) return HashMap.empty();
    Stream<ITweet> givenReplies = Stream.ofAll(this.getTwitterClient().searchForTweetsWithin7days(query, dates._1(), dates._2()));
    return givenReplies
        .filter(tweet -> tweet.getInReplyToUserId()!=null)
        .filter(tweet -> this.isUserInList(tweet.getInReplyToUserId()))
        .filter(tweet -> !this.getUserId().equals(this.getTwitterClient().getInitialTweet(tweet, true).getAuthorId()))
        .peek(tweet -> LOGGER.info("analyzing API recent reply : " + tweet.getText()))
        .map(tweet -> this.getTwitterClient().getInitialTweet(tweet, true))
        .filter(tweet -> tweet.getAuthorId()!=null)
        .groupBy(ITweet::getAuthorId)
        .map(this::getTupleAnswerGiven);
  }

  public Map<String, UserInteraction> countRecentRetweetsGiven(Date mostRecentArchiveTweetDate) {
    LOGGER.info("\nCounting rercent retweets from user (API)...");
    String       query  = "from:" + this.getUserName() + " is:retweet";
    Tuple2<Date, Date> dates = getDatesFromMostRecentTweetDate(mostRecentArchiveTweetDate);
    if(dates==null) return HashMap.empty();
    Stream<ITweet> givenRetweets = Stream.ofAll(this.getTwitterClient().searchForTweetsWithin7days(query, dates._1(), dates._2()));
    return givenRetweets
        .filter(tweet -> tweet.getInReplyToStatusId(TweetType.RETWEETED) != null)
        .peek(tweet -> LOGGER.info("analyzing API recent retweet : " + tweet.getText()))
        .filter(tweet -> this.isUserInList(this.getTwitterClient().getTweet(tweet.getInReplyToStatusId(TweetType.RETWEETED)).getAuthorId()))
        .groupBy(tweet -> this.getTwitterClient().getTweet(tweet.getInReplyToStatusId(TweetType.RETWEETED)).getAuthorId())
        .map(this::getTupleAnswerGiven);
  }

  public Map<String, UserInteraction> countRecentQuotesGiven(Date mostRecentArchiveTweetDate) {
    LOGGER.info("\nCounting rercent retweets from user (API)...");
    String       query  = "from:" + this.getUserName() + " is:quote";
    Tuple2<Date, Date> dates = getDatesFromMostRecentTweetDate(mostRecentArchiveTweetDate);
    if(dates==null) return HashMap.empty();
    Stream<ITweet> givenQuotes = Stream.ofAll(this.getTwitterClient().searchForTweetsWithin7days(query, dates._1(), dates._2()));
    return givenQuotes
        .filter(tweet -> tweet.getInReplyToStatusId(TweetType.QUOTED) != null)
        .peek(tweet -> LOGGER.info("analyzing API recent retweet : " + tweet.getText()))
        .filter(tweet -> this.isUserInList(this.getTwitterClient().getTweet(tweet.getInReplyToStatusId(TweetType.QUOTED)).getAuthorId()))
        .groupBy(tweet -> this.getTwitterClient().getTweet(tweet.getInReplyToStatusId(TweetType.QUOTED)).getAuthorId())
        .map(this::getTupleRetweetGiven);
  }

  private Tuple2<Date, Date> getDatesFromMostRecentTweetDate(Date mostRecentArchiveTweetDate){
    long delta = (System.currentTimeMillis() - mostRecentArchiveTweetDate.getTime()) / (1000 * 60 * 60 * 24);
    if (delta < 1) {
      return null;
    }
    Date         toDate;
    Date         fromDate;
    if (delta > 7) {
      toDate   = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.HOUR);
      fromDate = DateUtils.ceiling(DateUtils.addDays(toDate, -7), Calendar.HOUR);
    } else {
      toDate   = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.HOUR);
      fromDate = mostRecentArchiveTweetDate;
    }

    return Tuple.of(fromDate, toDate);
  }

  /**
   * Count the replies received by the user from others
   * @param currentWeek if true, from D-7 until D, otherwise from D-30 until D-7
   * @return a map with tweet id as key and TweetInteraction as value
   */
  public Map<String, TweetInteraction> countRepliesReceived(boolean currentWeek) {
    LOGGER.info("Counting replies received - currentWeek = " + currentWeek + " ...");
    return this.getTweetsWithRepliesStream(currentWeek, false)
               .filter(tweet -> this.isUserInList(tweet.getAuthorId()))
               .filter(tweet -> this.getUserId().equals(this.getTwitterClient().getInitialTweet(tweet, true).getAuthorId()))
               .peek(initialTweet -> LOGGER.info("analyzing API reply : " + initialTweet.getText()))
               .groupBy(tweet ->  this.getTwitterClient().getInitialTweet(tweet, true).getId())
               .map(this::getTurpleAnswerReceived);
  }

  public Map<String, TweetInteraction> countQuotesReceived(boolean currentWeek) {
    LOGGER.info("Counting replies received - currentWeek = " + currentWeek + " ...");
    return this.getTweetsWithRepliesStream(currentWeek, true)
               .filter(tweet -> this.isUserInList(tweet.getAuthorId()))
               .peek(tweet -> LOGGER.info("analyzing API reply : " + tweet.getText()))
               .filter(tweet -> this.getUserId().equals(this.getTwitterClient().getTweet(tweet.getInReplyToStatusId(TweetType.QUOTED)).getAuthorId()))
               .groupBy(tweet ->  tweet.getInReplyToStatusId(TweetType.QUOTED))
               .map(this::getTupleRetweetReceived);
  }


  private Stream<ITweet> getTweetsWithRepliesStream(boolean currentWeek, boolean quotes){
    String mentionQuery = "to:" + this.getUserName();
    if(!quotes){
      mentionQuery += " has:mentions";
    } else{
      mentionQuery += " is:quote";
    }
    if (currentWeek) {
      Date toDate = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.HOUR);
      return  Stream.ofAll(this.getTwitterClient()
                               .searchForTweetsWithin7days("(" + mentionQuery + ")"  + "OR (url:"+this.getUserName()+" -is:retweet)",
                                                           DateUtils.ceiling(DateUtils.addDays(toDate, -7), Calendar.HOUR),
                                                           toDate));
    } else{
      Date toDate = DateUtils.truncate(ConverterHelper.dayBeforeNow(7), Calendar.DAY_OF_MONTH);
      return Stream.ofAll(this.getTwitterClient()
                              .searchForTweetsWithin30days(mentionQuery,
                                                           DateUtils.ceiling(DateUtils.addDays(toDate, -23), Calendar.DAY_OF_MONTH),
                                                           toDate));
    }
  }

  /**
   * Count the number of likes given to initial status (excluding answers)
   * @return a map with userId as String and UsereInteraction as value
   */
  public Map<String, UserInteraction> countGivenLikesOnStatuses() {
    LOGGER.info("\nCounting given likes excluding answers...");

    Stream<ITweet> likedTweets = Stream.ofAll(this.getTwitterClient().getFavorites(this.getUserId(), 5000));
    return likedTweets
        .filter(tweet -> tweet.getInReplyToStatusId() == null)
        .filter(tweet -> this.isUserInList(tweet.getAuthorId()))
        .peek(tweet -> LOGGER.info("analyzing tweet : " + tweet.getText()))
        .groupBy(ITweet::getAuthorId)
        .map(this::getTupleLikeGiven);
  }

  /**
   * Get a score based on the average interaction ratio on the last 7 days tweets (max: 100 tweets)
   * @param user the user that should be analyzed
   * @return a score
   */
  public int getInteractionScore(IUser user){
    String query = "from:" + user.getName() + " -is:reply -is:retweet";
    Date fromDate = DateUtils.truncate(ConverterHelper.dayBeforeNow(6), Calendar.DAY_OF_MONTH);
    Date toDate   = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.DAY_OF_MONTH);
    List<ITweet> lastUserTweets = this.getTwitterClient().searchForTweetsWithin7days(query, fromDate, toDate);
    int nbFollowers = user.getFollowersCount();
    if(lastUserTweets.size()==0 || nbFollowers==0) return 0;
    // RT : 4 pts / Quote : 3 pts / Reply : 2 pts / like : 1 pt
    int points = lastUserTweets.stream()
                          .map(tweet -> 4*tweet.getRetweetCount() + 3*tweet.getQuoteCount() + 2*tweet.getReplyCount() + tweet.getLikeCount())
                                .mapToInt(Integer::intValue)
                                .sum();

    return 10000*points/lastUserTweets.size()/nbFollowers;
  }

  public int getNbTweetsWithin7Days(IUser user){
    String query = "from:" + user.getName() + " -is:reply -is:retweet";
    Date fromDate = DateUtils.truncate(ConverterHelper.dayBeforeNow(6), Calendar.DAY_OF_MONTH); // @todo to improve
    Date toDate   = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.DAY_OF_MONTH);
    return this.getTwitterClient().searchForTweetsWithin7days(query, fromDate, toDate).size();
  }

  public int getMedianInteractionScore(IUser user){
    String query = "from:" + user.getName() + " -is:reply -is:retweet";
    Date fromDate = DateUtils.truncate(ConverterHelper.dayBeforeNow(6), Calendar.DAY_OF_MONTH);
    Date toDate   = DateUtils.truncate(ConverterHelper.minutesBeforeNow(120), Calendar.DAY_OF_MONTH);
    List<ITweet> lastUserTweets = this.getTwitterClient().searchForTweetsWithin7days(query, fromDate, toDate);
    List<Integer> scores = new ArrayList<>();
    if(lastUserTweets.size()==0) return 0;
    // RT : 4 pts / Quote : 3 pts / Reply : 2 pts / like : 1 pt
    scores = lastUserTweets.stream()
                               .map(tweet -> 4*tweet.getRetweetCount() + 3*tweet.getQuoteCount() + 2*tweet.getReplyCount() + tweet.getLikeCount())
                               .collect(Collectors.toList());
    int size = scores.size();
    double result = scores.stream().sorted()
        .skip((size-1)/2).limit(2-size%2).mapToInt(x->x).average().orElse(0.0);
    return (int)result;
  }

}