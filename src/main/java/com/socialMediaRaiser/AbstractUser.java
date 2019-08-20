package com.socialMediaRaiser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractUser {
    private String id;
    private String userName;
    private int followersCount;
    private int followingsCount;

    public double getFollowersRatio() {
        return (double) this.followersCount / (double) this.followingsCount;
    }

    public abstract boolean shouldBeFollowed(String ownerName);

    public abstract boolean shouldBeUnfollowed();

    @Override
    public boolean equals(Object o) {
        AbstractUser otherUser = (AbstractUser) o;
        return otherUser.getId() == this.getId();
    }
}
