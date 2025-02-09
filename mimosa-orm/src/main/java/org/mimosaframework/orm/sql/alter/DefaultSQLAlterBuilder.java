package org.mimosaframework.orm.sql.alter;

import org.mimosaframework.orm.sql.AbstractSQLBuilder;
import org.mimosaframework.orm.sql.stamp.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DefaultSQLAlterBuilder
        extends
        AbstractSQLBuilder
        implements
        RedefineAlterBuilder {

    protected StampAlter stampAlter = new StampAlter();
    protected List<StampAlterItem> items = new ArrayList<>();

    private StampAlterItem getLastItem() {
        StampAlterItem item = this.items.get(this.items.size() - 1);
        return item;
    }

    @Override
    public DefaultSQLAlterBuilder alter() {
        this.addPoint("alter");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder name(String value) {
        this.gammars.add("name");
        if (this.previous("database")) {
            this.stampAlter.databaseName = value;
        }
        if (this.point.equals("add") && this.getPointNext(1).equals("primary")
                && this.getPointNext(2).equals("key")) {
            StampAlterItem item = this.getLastItem();
            item.indexName = value;
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder charset(String charset) {
        this.gammars.add("charset");
        this.stampAlter.charset = charset;
        if ("alter".equals(this.getPrePoint()) && this.point.equals("table")) {
            StampAlterItem item = new StampAlterItem();
            this.stampAlter.target = KeyTarget.TABLE;
            item.action = KeyAction.CHARACTER_SET;
            item.charset = charset;
            this.items.add(item);
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder collate(String collate) {
        this.gammars.add("collate");
        this.stampAlter.collate = collate;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder database() {
        this.addPoint("database");
        this.stampAlter.target = KeyTarget.DATABASE;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder column(Serializable field) {
        this.gammars.add("column");
        StampAlterItem item = this.getLastItem();
        if (this.previous("after")) {
            item.after = new StampColumn(field);
        } else if (this.previous("before")) {
            item.before = new StampColumn(field);
        } else if (this.getPointNext(1).equals("index")
                || this.getPointNext(1).equals("fullText")
                || this.getPointNext(1).equals("unique")
                || (this.getPointNext(1).equals("primary") && this.getPointNext(2).equals("key"))) {
            item.columns = new StampColumn[]{new StampColumn(field)};
        } else {
            item.column = new StampColumn(field);
            item.struct = KeyAlterStruct.COLUMN;

            if (this.point.equals("drop")) {
                item.dropType = KeyAlterDropType.COLUMN;
            }
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder columns(Serializable... fields) {
        this.gammars.add("columns");
        StampColumn[] columns = new StampColumn[fields.length];
        int i = 0;
        for (Serializable field : fields) {
            columns[i] = new StampColumn(field);
            i++;
        }
        StampAlterItem item = this.getLastItem();
        item.columns = columns;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder table(Class table) {
        this.addPoint("table");
        this.stampAlter.target = KeyTarget.TABLE;
        this.stampAlter.tableClass = table;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder after() {
        this.gammars.add("after");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder before() {
        this.gammars.add("before");
        return this;
    }

    @Override
    public void first() {
        StampAlterItem item = this.getLastItem();
        item.first = true;
        this.gammars.add("first");
    }

    @Override
    public DefaultSQLAlterBuilder autoIncrement() {
        this.gammars.add("autoIncrement");
        if (this.previous("table")) {
            StampAlterItem item = new StampAlterItem();
            item.action = KeyAction.AUTO_INCREMENT;
            this.items.add(item);
        } else {
            StampAlterItem item = this.getLastItem();
            item.autoIncrement = KeyConfirm.YES;
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder column() {
        this.gammars.add("column");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder add() {
        this.addPoint("add");
        StampAlterItem item = new StampAlterItem();
        item.action = KeyAction.ADD;
        this.items.add(item);
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder intType() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.INT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder varchar(int len) {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.VARCHAR;
        item.len = len;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder charType(int len) {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.CHAR;
        item.len = len;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder blob() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.BLOB;
        return this;
    }

    @Override
    public Object mediumBlob() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.MEDIUMBLOB;
        return this;
    }

    @Override
    public Object longBlob() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.LONGBLOB;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder text() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.TEXT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder json() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.JSON;
        return this;
    }

    @Override
    public Object mediumText() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.MEDIUMTEXT;
        return this;
    }

    @Override
    public Object longText() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.LONGTEXT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder tinyint() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.TINYINT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder smallint() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.SMALLINT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder bigint() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.BIGINT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder floatType() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.FLOAT;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder doubleType() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.DOUBLE;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder decimal(int len, int scale) {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.DECIMAL;
        item.len = len;
        item.scale = scale;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder booleanType() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.BOOLEAN;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder date() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.DATE;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder time() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.TIME;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder datetime() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.DATETIME;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder timestamp() {
        this.gammars.add("type");
        StampAlterItem item = this.getLastItem();
        item.columnType = KeyColumnType.TIMESTAMP;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder comment(String comment) {
        this.gammars.add("comment");
        StampAlterItem item = this.getLastItem();
        item.comment = comment;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder drop() {
        this.addPoint("drop");
        StampAlterItem item = new StampAlterItem();
        item.action = KeyAction.DROP;
        this.items.add(item);
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder key() {
        this.gammars.add("key");
        if (this.previous("primary")) {
            StampAlterItem item = this.getLastItem();
            if (this.getPointNext(1).equals("column")) {
                item.pk = KeyConfirm.YES;
            } else if (this.point.equals("add")
                    && !this.getPointNext(1).equals("column")) {
                item.struct = KeyAlterStruct.PRIMARY_KEY;
            }
            if (this.point.equals("drop")) {
                item.dropType = KeyAlterDropType.PRIMARY_KEY;
            }
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder primary() {
        this.gammars.add("primary");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder to() {
        this.gammars.add("to");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder modify() {
        this.addPoint("modify");
        StampAlterItem item = new StampAlterItem();
        item.action = KeyAction.MODIFY;
        this.items.add(item);
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder value(int number) {
        this.gammars.add("value");
        if (this.point.equals("table")) {
            StampAlterItem item = this.getLastItem();
            item.value = number;
        }
        return this;
    }

    @Override
    public StampAlter compile() {
        if (this.items != null && this.items.size() > 0) {
            this.stampAlter.items = this.items.toArray(new StampAlterItem[]{});
        }
        return this.stampAlter;
    }

    @Override
    public DefaultSQLAlterBuilder not() {
        this.gammars.add("not");
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder nullable() {
        this.gammars.add("nullable");
        if (this.previous("not")) {
            StampAlterItem item = this.getLastItem();
            item.nullable = KeyConfirm.NO;
        }
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder defaultValue(String value) {
        this.gammars.add("defaultValue");
        StampAlterItem item = this.getLastItem();
        item.defaultValue = value;
        return this;
    }

    @Override
    public DefaultSQLAlterBuilder table(String name) {
        this.addPoint("table");
        this.stampAlter.target = KeyTarget.TABLE;
        this.stampAlter.tableName = name;
        return this;
    }

    public DefaultSQLAlterBuilder timeForUpdate() {
        StampAlterItem item = this.getLastItem();
        if (item != null) {
            item.timeForUpdate = true;
        }
        return this;
    }
}
