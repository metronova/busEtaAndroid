package com.google.android.gms.location.sample.locationupdates;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    public static String returnDatetimeString(String string) {

        Date date = stringToDate(string);

        String outputString = "";

        int year = date.getYear()+1900;
        int month = date.getMonth()+1;
        int day = date.getDay();

        outputString = year + "-" + month + "-" + date.getDay();
        outputString += " " + date.getHours() + ":" + date.getMinutes();

        return outputString;
    }

    public static String returnTimeString(String string) {

        Date date = stringToDate(string);

        String outputString = "";
        outputString = date.getHours() + ":" + date.getMinutes();

        return outputString;
    }

    public static Date stringToDate(String string) {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            date = dateFormat.parse(string);
        } catch (
                ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

}
