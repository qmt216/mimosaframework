package org.mimosaframework.orm.sql.insert;

import org.mimosaframework.orm.mapping.MappingTable;
import org.mimosaframework.orm.platform.SQLMappingField;
import org.mimosaframework.orm.platform.SQLMappingTable;
import org.mimosaframework.orm.sql.*;

import java.io.Serializable;

public abstract class AbstractSQLInsertBuilder
        extends
        AbstractSQLBuilder

        implements
        RedefineInsertBuilder {

    protected Serializable[] fields;

    @Override
    public Object insert() {
        this.sqlBuilder.INSERT();
        return this;
    }


    @Override
    public Object columns(Serializable... fields) {
        this.sqlBuilder.addParenthesisStart();
        if (fields != null) {
            int i = 0;
            for (Serializable f : fields) {
                this.sqlBuilder.addMappingField(new SQLMappingField(f));
                i++;
                if (i != fields.length) this.sqlBuilder.addSplit();
            }
        }
        this.sqlBuilder.addParenthesisEnd();
        this.fields = fields;
        return this;
    }

    @Override
    public Object table(Class table) {
        this.sqlBuilder.addMappingTable(new SQLMappingTable(table));
        return this;
    }

    @Override
    public Object into() {
        this.sqlBuilder.INTO();
        return this;
    }

    @Override
    public Object split() {
        this.sqlBuilder.addSplit();
        return this;
    }

    @Override
    public Object values() {
        this.sqlBuilder.VALUES();
        return this;
    }

    @Override
    public Object row(Object... values) {
        if (values != null) {
            int i = 0;
            this.sqlBuilder.addParenthesisStart();
            for (Object v : values) {
                Serializable field = this.fields[i];
                this.sqlBuilder.addDataPlaceholder(field.toString(), v);
                i++;
                if (i != values.length) this.sqlBuilder.addSplit();
            }
            this.sqlBuilder.addParenthesisEnd();
        }
        return this;
    }
}
