package com.google.android.gms.location.sample.locationupdates;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StopEta {

    String co;
    String route;
    String dir;
    String serviceType;
    String seq;
    String destTc;
    String destSc;
    String destEn;
    String etaSeq;
    String eta;
    String rmkTc;
    String rmkSc;
    String rmkEn;
    String dataTimestamp;
    String etaTimeLeft;

    public String getCo() {
        return co;
    }

    public void setCo(String co) {
        this.co = co;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public String getDestTc() {
        return destTc;
    }

    public void setDestTc(String destTc) {
        this.destTc = destTc;
    }

    public String getDestSc() {
        return destSc;
    }

    public void setDestSc(String destSc) {
        this.destSc = destSc;
    }

    public String getDestEn() {
        return destEn;
    }

    public void setDestEn(String destEn) {
        this.destEn = destEn;
    }

    public String getEtaSeq() {
        return etaSeq;
    }

    public void setEtaSeq(String etaSeq) {
        this.etaSeq = etaSeq;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public String getRmkTc() {
        return rmkTc;
    }

    public void setRmkTc(String rmkTc) {
        this.rmkTc = rmkTc;
    }

    public String getRmkSc() {
        return rmkSc;
    }

    public void setRmkSc(String rmkSc) {
        this.rmkSc = rmkSc;
    }

    public String getRmkEn() {
        return rmkEn;
    }

    public void setRmkEn(String rmkEn) {
        this.rmkEn = rmkEn;
    }

    public String getDataTimestamp() {
        return dataTimestamp;
    }

    public void setDataTimestamp(String dataTimestamp) {
        this.dataTimestamp = dataTimestamp;
    }

    public String getEtaTimeLeft() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date etaDate = dateFormat.parse(this.eta);
        Date currentTime = new Date();

        Date timeLeft = new Date(etaDate.getTime() - currentTime.getTime());

        //System.out.println(timeLeft.toString());

        etaTimeLeft = String.valueOf(timeLeft.getMinutes());

        return etaTimeLeft;
    }

    public void setEtaTimeLeft(String etaTimeLeft) {
        this.etaTimeLeft = etaTimeLeft;
    }

    @Override
    public String toString() {
       /* return "StopEta{" +
                "co='" + co + '\'' +
                ", route='" + route + '\'' +
                ", dir='" + dir + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", seq='" + seq + '\'' +
                ", descTc='" + destTc + '\'' +
                ", destSc='" + destSc + '\'' +
                ", destEn='" + destEn + '\'' +
                ", etaSeq='" + etaSeq + '\'' +
                ", eta='" + eta + '\'' +
                ", rmkTc='" + rmkTc + '\'' +
                ", rmkSc='" + rmkSc + '\'' +
                ", rmkEn='" + rmkEn + '\'' +
                ", dataTimestamp='" + dataTimestamp + '\'' +
                '}';*/
        String etaLeft = null;
        try {
            etaLeft = this.getEtaTimeLeft();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String etaTime = DateUtil.returnTimeString(this.eta);

        /*return "route='" + route + "', " +
                //"\n" +
                "descTc='" + destTc + "', " +
                //"\n" +
                "eta='" + etaLeft + "'";*/

        return co + " " + route + " " + dir + " " + serviceType + " "+ destTc + " " + etaTime + " " + etaLeft + "min(s)";
    }
}
