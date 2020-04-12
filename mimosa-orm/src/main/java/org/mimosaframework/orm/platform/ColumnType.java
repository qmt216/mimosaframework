package org.mimosaframework.orm.platform;

import org.mimosaframework.orm.sql.stamp.KeyColumnType;

public class ColumnType {
    private KeyColumnType type;
    private String typeName;
    private int length;
    private int scale;

    public ColumnType(KeyColumnType type, String typeName, int length, int scale) {
        this.type = type;
        this.typeName = typeName;
        this.length = length;
        this.scale = scale;
    }

    public KeyColumnType getType() {
        return type;
    }

    public void setType(KeyColumnType type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }
}