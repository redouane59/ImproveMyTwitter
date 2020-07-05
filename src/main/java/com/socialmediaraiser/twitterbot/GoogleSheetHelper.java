package com.socialmediaraiser.twitterbot;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.socialmediaraiser.twitter.TwitterClient;
import com.socialmediaraiser.twitter.signature.TwitterCredentials;
import com.socialmediaraiser.twitterbot.impl.User;
import com.socialmediaraiser.twitterbot.properties.GoogleCredentials;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;
import lombok.Data;
import lombok.Getter;


@Getter
@CustomLog
public class GoogleSheetHelper {

  private Sheets               sheetsService;
  private String               sheetId;
  private String               tabName;

  public GoogleSheetHelper() throws IOException {
    URL googleCredentialsUrl = GoogleCredentials.class.getClassLoader().getResource("google-credentials.json");
    if(googleCredentialsUrl==null){
      LOGGER.severe("google-credentials.json file not found in src/main/resources");
      return;
    }
    GoogleCredentials googleCredentials = TwitterClient.OBJECT_MAPPER.readValue(googleCredentialsUrl, GoogleCredentials.class);

    this.sheetId            = googleCredentials.getSheetId();
    this.tabName            = googleCredentials.getTabName();

    try {
      this.sheetsService = SheetsServiceUtil.getSheetsService();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }


  public void addAllFollowers(List<User> users) {
    for (User user : users) {
      this.addNewFollowerLineSimple(user);
      try {
        TimeUnit.MILLISECONDS.sleep(100);
      } catch (InterruptedException e) {
        LOGGER.severe(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }
  }

  public void addNewFollowerLineSimple(User user) {

    ValueRange body = new ValueRange()
        .setValues(Arrays.asList(Arrays.asList(
            String.valueOf(user.getId()),
            user.getName(),
            user.getFollowersCount(),
            user.getFollowingCount(),
            user.getTweetCount(),
            user.getDescription().
                replace("\"", " ")
                .replace(";", " ")
                .replace("\n", " "),
            Optional.ofNullable(user.getLocation()).orElse(""),
            user.getNbRepliesReceived(),
            user.getNbRetweetsReceived(),
            user.getNbRepliesGiven(),
            user.getNbRetweetsGiven(),
            user.getNbLikesGiven(),
            user.isFollowing()
        )));
    try {
      Sheets.Spreadsheets.Values.Append request =
          sheetsService.spreadsheets().values().append(this.sheetId, this.tabName + "!A1", body);
      request.setValueInputOption("RAW");
      request.execute();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }

  public void addNewFollowerLineSimple(List<User> users) {
    List<List<Object>> values = new ArrayList<>();
    for (User user : users) {
      values.add(Arrays.asList(String.valueOf(user.getId()),
                               user.getName(),
                               user.getFollowersCount(),
                               user.getFollowingCount(),
                               user.getTweetCount(),
                               user.getDescription().
                                   replace("\"", " ")
                                   .replace(";", " ")
                                   .replace("\n", " "),
                               Optional.ofNullable(user.getLocation()).orElse(""),
                               user.getNbRepliesReceived(),
                               user.getNbRetweetsReceived(),
                               user.getNbRepliesGiven(),
                               user.getNbRetweetsGiven(),
                               user.getNbLikesGiven(),
                               user.isFollowing()));
    }
    ValueRange body = new ValueRange()
        .setValues(values);
    try {
      Sheets.Spreadsheets.Values.Append request =
          sheetsService.spreadsheets().values().append(this.sheetId, this.tabName + "!A1", body);
      request.setValueInputOption("RAW");
      request.execute();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      try {
        TimeUnit.SECONDS.sleep(10);
        this.addNewFollowerLineSimple(users);
      } catch (InterruptedException e2) {
        LOGGER.severe(e2.getMessage());
        Thread.currentThread().interrupt();
      }
    }

  }
}
