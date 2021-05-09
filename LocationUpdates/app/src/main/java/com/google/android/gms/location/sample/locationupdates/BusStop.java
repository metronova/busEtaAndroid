package com.google.android.gms.location.sample.locationupdates;

import static java.lang.Math.round;

public class BusStop {

    String stopID;
    String nameEn;
    String nameTc;
    String nameSc;
    String lat;
    String lon;
    String distance;

    public String getStopID() {
        return stopID;
    }

    public void setStopID(String stopID) {
        this.stopID = stopID;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameTc() {
        return nameTc;
    }

    public void setNameTc(String nameTc) {
        this.nameTc = nameTc;
    }

    public String getNameSc() {
        return nameSc;
    }

    public void setNameSc(String nameSc) {
        this.nameSc = nameSc;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }


    @Override
    public String toString() {
       /* return "BusStop{" +
                "stop='" + stop + '\'' +
                ", nameEn='" + nameEn + '\'' +
                ", nameTc='" + nameTc + '\'' +
                ", nameSc='" + nameSc + '\'' +
                ", lat='" + lat + '\'' +
                ", lon='" + lon + '\'' +
                ", distance='" + distance + '\'' +
                '}';*/
         /*return "nameTc='" + nameTc + "'," +
                 "\n" +
                "distance='" + distance  + "'" ;*/

        double distanceMeter = Double.valueOf(distance) * 111.139 * 1000;

        return nameTc + " " + round(distanceMeter) + "m";
    }

}
