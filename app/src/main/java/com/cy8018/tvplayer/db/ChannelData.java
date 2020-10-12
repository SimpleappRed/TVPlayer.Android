package com.cy8018.tvplayer.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "channels")
@TypeConverters({ListConverter.class, DateConverter.class})
public class ChannelData {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "logo")
    public String logo;

    @ColumnInfo(name = "countryCode")
    public String countryCode;

    @ColumnInfo(name = "countryName")
    public String countryName;

    @ColumnInfo(name = "languageName")
    public String languageName;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "url")
    public ArrayList<String> url;

    @ColumnInfo(name = "last_play_time")
    public Date lastPlayTime;

    @ColumnInfo(name = "last_source")
    public int lastSource;

    @ColumnInfo(name = "is_favorite")
    public boolean isFavorite;

}
