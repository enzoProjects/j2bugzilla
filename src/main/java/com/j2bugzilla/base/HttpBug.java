package com.j2bugzilla.base;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
/**
 * Created by springfield-home on 5/28/17.
 */
public class HttpBug {
    private Map<String, Object> internalState;

    HttpBug(Map<String, Object> internalState) {
        this.internalState = internalState;
    }

    public Map<String,Object> getInternalState() {
        return internalState;
    }

    public void setInternalState(Map<String, Object> internalState) {
        this.internalState = internalState;
    }

    public Date getDeltaTs() {
        // ex :2017-05-26 13:38:57
        String sLastChange = (String) internalState.get("DeltaTs");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = format.parse(sLastChange);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getId() {
        return (String) internalState.get("Id");
    }
}
