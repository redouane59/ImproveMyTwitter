package com.socialmediaraiser.twitterbot.integration;

import com.socialmediaraiser.twitterbot.FollowProperties;
import com.socialmediaraiser.twitterbot.GoogleSheetHelper;
import com.socialmediaraiser.twitterbot.RandomForestAlgoritm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleSheetHelperTest {

    private static String ownerName = "RedTheOne";
    private GoogleSheetHelper googleSheetHelper = new GoogleSheetHelper(ownerName);

    @BeforeAll
    static void init(){
        FollowProperties.load(ownerName);
    }

  /*  @Test
    void testAddNewFollowerLineSimple(){
        User user = User.builder()
                .id("12345")
                .followersCout(10)
                .followingCount(50)
                .userName("Red")
                .description("just me")
                .statusesCount(1000)
                .location("Roubaix").build();
        googleSheetHelper.addNewFollowerLineSimple(user);
        //@todo remove line
    } */

    @Test
    void testGetPreviouslyFollowedIdsAll(){
        List<String> result = googleSheetHelper.getPreviouslyFollowedIds();
        assertTrue(result.size()>200);
    }

    @Test
    void testGetPreviouslyFollowedIdsByDate(){
        Date date = new Date();
        date.setDate(11);
        date.setMonth(04);
        List<String> result = googleSheetHelper.getPreviouslyFollowedIds(true, true, date);
        assertTrue(result.size()>50);
    }

    @Test
    void testRowOfUser(){
        int result = googleSheetHelper.getUserRows().get("925955978");
        assertEquals(10, result);
        result = googleSheetHelper.getUserRows().get("1719824233");
        assertEquals(3493, result);
    }

    @Test
    void testGetRandomForestData() throws Exception {
        RandomForestAlgoritm.process();
        List<List<Object>> result = googleSheetHelper.getRandomForestData();
        assertFalse(result.isEmpty());
    }
}
