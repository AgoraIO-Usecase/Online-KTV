package com.agora.data.model;

import androidx.annotation.NonNull;

import com.agora.data.ExampleData;
import com.agora.data.R;

import java.io.Serializable;

public class User implements Serializable, Cloneable {
    public static final String TABLE_NAME = "USER";

    public static final String COLUMN_ID = "objectId";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AVATAR = "avatar";
    public static final String COLUMN_CREATEDAT = "createdAt";

    private String objectId;
    private String name;
    private String avatar;

    public User() {
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return objectId.equals(user.objectId);
    }

    @Override
    public int hashCode() {
        return objectId.hashCode();
    }

    @NonNull
    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new User();
    }

    public int getAvatarRes() {
        int temp = 0;
        try {
            temp = Integer.parseInt(avatar);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        if(temp < 0 || temp >= ExampleData.exampleAvatars.size())
            temp = 0;
        return ExampleData.exampleAvatars.get(temp);
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "objectId='" + objectId + '\'' +
                ", name='" + name + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
