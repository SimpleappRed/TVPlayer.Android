package com.cy8018.tvplayer.db;

import androidx.room.TypeConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateConverter {
    @TypeConverter
    public static Date revertDate(String value)  {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = simpleDateFormat.parse(value);
            return date;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @TypeConverter
    public static String converterDate(Date value) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return simpleDateFormat.format(value);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
