package com.socialmediaraiser.twitter.helpers.dto.getuser;

import lombok.Data;

import java.util.List;

@Data
public class IncludesDTO {
    private List<TweetDTO> tweets;
}
