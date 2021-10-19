package com.agora.data.model;

import androidx.annotation.NonNull;

import com.agora.data.ExampleData;
import com.agora.data.R;

import java.io.Serializable;

public class User implements Serializable, Cloneable {

    private int userId;
    private String name;
    private String avatar;

    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int objectId) {
        this.userId = objectId;
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

        return this.userId == user.userId;
    }

    @Override
    public int hashCode() {
        int res = 17;
        res = res * 31 + name.hashCode();
        res = res * 31 + userId;
        return res;
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
                "objectId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
