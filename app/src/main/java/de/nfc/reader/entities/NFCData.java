package de.nfc.reader.entities;

import java.io.Serializable;

/**
 * Created by teguh.santoso on 25.12.2016.
 */

public class NFCData implements Serializable {
    private static final long serialVersionUID = -3496408271787886660L;
    private String tagId;
    private String timestamp;

    public NFCData(String tagId, String timestamp) {
        this.tagId = tagId;
        this.timestamp = timestamp;
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
