package com.primesprint.pii.dto;

public class SensitiveDataItem {
    private String type;
    private String value;
    private String source; // "document" | "prompt"
    private int start;     // UTF-8 byte offset (0-based)
    private int end;       // exclusive

    public SensitiveDataItem() {
    }

    public SensitiveDataItem(String type, String value, String source, int start, int end) {
        this.type = type;
        this.value = value;
        this.source = source;
        this.start = start;
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}

