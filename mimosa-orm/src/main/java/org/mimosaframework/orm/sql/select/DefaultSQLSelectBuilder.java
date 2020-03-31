package org.mimosaframework.orm.sql.select;

import org.mimosaframework.orm.sql.*;
import org.mimosaframework.orm.sql.stamp.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DefaultSQLSelectBuilder
        extends
        AbstractSQLBuilder
        implements
        RedefineSelectBuilder {

    protected StampSelect stampSelect = new StampSelect();
    protected List<StampSelectField> stampSelectFields = new ArrayList<>();
    protected List<StampFrom> stampFroms = new ArrayList<>();

    protected List<StampSelectJoin> joins = new ArrayList<>();
    protected StampSelectJoin lastJoin = null;

    protected StampWhere where = null;
    protected StampWhere having = null;
    protected StampWhere lastWhere = null;
    protected List<StampOrderBy> orderBys = new ArrayList<>();
    protected StampOrderBy lastOrderBy = null;
    protected List<StampColumn> groupBy = new ArrayList<>();

    protected StampSelectField getLastField() {
        if (this.stampSelectFields.size() > 0) {
            return this.stampSelectFields.get(this.stampSelectFields.size() - 1);
        }
        return null;
    }

    @Override
    public Object select() {
        this.gammars.add("select");
        return this;
    }

    @Override
    public Object all() {
        this.gammars.add("all");
        StampSelectField field = new StampSelectField();
        field.fieldType = KeyFieldType.ALL;
        stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object field(Serializable... fields) {
        this.gammars.add("field");
        int i = 0;
        for (Serializable f : fields) {
            StampSelectField field = new StampSelectField();
            field.fieldType = KeyFieldType.COLUMN;
            field.column = new StampColumn(f);
            stampSelectFields.add(field);
            if (i == 0) {
                this.setKeyword2SelectField(field);
            }
            i++;
        }
        return this;
    }

    @Override
    public Object field(Class table, Serializable... fields) {
        this.gammars.add("field");
        int i = 0;
        for (Serializable f : fields) {
            StampSelectField field = new StampSelectField();
            field.fieldType = KeyFieldType.COLUMN;
            field.column = new StampColumn(table, f);
            stampSelectFields.add(field);
            if (i == 0) {
                this.setKeyword2SelectField(field);
            }
            i++;
        }
        return this;
    }

    @Override
    public Object field(String tableAliasName, Serializable... fields) {
        this.gammars.add("field");
        int i = 0;
        for (Serializable f : fields) {
            StampSelectField field = new StampSelectField();
            field.fieldType = KeyFieldType.COLUMN;
            field.column = new StampColumn(tableAliasName, f);
            stampSelectFields.add(field);
            if (i == 0) {
                this.setKeyword2SelectField(field);
            }
            i++;
        }
        return this;
    }

    @Override
    public Object field(Serializable field, String fieldAliasName) {
        this.gammars.add("field");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(field);
        stampSelectFields.add(selectField);
        this.setKeyword2SelectField(selectField);
        return this;
    }

    @Override
    public Object field(Class table, Serializable field, String fieldAliasName) {
        this.gammars.add("field");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(table, field);
        stampSelectFields.add(selectField);
        this.setKeyword2SelectField(selectField);
        return this;
    }

    @Override
    public Object field(String tableAliasName, Serializable field, String fieldAliasName) {
        this.gammars.add("field");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(tableAliasName, field);
        stampSelectFields.add(selectField);
        this.setKeyword2SelectField(selectField);
        return this;
    }

    private void setKeyword2SelectField(StampSelectField selectField) {
        if (this.previous("distinct")) {
            // selectField.distinct = true;
        }
    }

    @Override
    public Object distinct(Serializable field) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.column = new StampColumn(field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    @Override
    public Object distinct(String tableAliasName, Serializable field) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.column = new StampColumn(tableAliasName, field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    @Override
    public Object distinct(Class table, Serializable field) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.column = new StampColumn(table, field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    @Override
    public Object distinct(Serializable field, String fieldAliasName) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    @Override
    public Object distinct(String tableAliasName, Serializable field, String fieldAliasName) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(tableAliasName, field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    @Override
    public Object distinct(Class table, Serializable field, String fieldAliasName) {
        this.gammars.add("distinct");
        StampSelectField selectField = new StampSelectField();
        selectField.fieldType = KeyFieldType.COLUMN;
        selectField.aliasName = fieldAliasName;
        selectField.column = new StampColumn(table, field);
        selectField.distinct = true;
        stampSelectFields.add(selectField);
        return this;
    }

    protected String[] skips = new String[]{"orderBy", "groupBy", "having", "from", "where", "join"};

    @Override
    public Object column(Serializable field) {
        this.gammars.add("column");
        this.column(null, null, field);
        return this;
    }

    @Override
    public Object column(Class table, Serializable field) {
        this.gammars.add("column");
        this.column(table, null, field);
        return this;
    }

    @Override
    public Object column(String aliasName, Serializable field) {
        this.gammars.add("column");
        this.column(null, aliasName, field);
        return this;
    }

    protected void column(Class table, String aliasName, Serializable field) {
        StampColumn column = null;
        if (table != null) column = new StampColumn(table, field);
        else if (aliasName != null) column = new StampColumn(aliasName, field);
        else column = new StampColumn(field);

        if (this.hasPreviousStops(new String[]{"orderBy"}, skips)) {
            StampOrderBy stampOrderBy = new StampOrderBy();
            stampOrderBy.column = column;
            this.lastOrderBy = stampOrderBy;
            this.orderBys.add(stampOrderBy);
        } else if (this.hasPreviousStops(new String[]{"groupBy"}, skips)) {
            this.groupBy.add(column);
        } else if (this.hasPreviousStops(new String[]{"having"}, skips)) {
            StampWhere where = new StampWhere();
            where.column = column;

            if (this.having == null) {
                this.having = where;
                this.lastWhere = where;
            } else {
                if (this.lastWhere != null) this.lastWhere.next = where;
                this.lastWhere = where;
            }
        } else {
            StampWhere where = new StampWhere();
            where.column = column;

            if (this.lastWhere != null) this.lastWhere.next = where;
            this.lastWhere = where;
            if (this.hasPreviousStops(new String[]{"join"}, skips)) {
            } else {
                if (this.where == null) {
                    this.where = where;
                }
            }
        }
    }

    @Override
    public Object and() {
        this.gammars.add("and");
        this.lastWhere.nextLogic = KeyLogic.AND;
        return this;
    }

    @Override
    public Object from() {
        this.gammars.add("from");
        return this;
    }

    @Override
    public Object having() {
        this.gammars.add("having");
        return this;
    }

    @Override
    public Object limit(int pos, int len) {
        this.gammars.add("limit");
        this.stampSelect.limit = new StampLimit(pos, len);
        return this;
    }

    @Override
    public Object isNull(Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNull";
        this.lastWhere.compareFun = new StampFieldFun("isNull", new StampColumn(field));
        return this;
    }

    @Override
    public Object isNull(Class table, Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNull";
        this.lastWhere.compareFun = new StampFieldFun("isNull", new StampColumn(table, field));
        return this;
    }

    @Override
    public Object isNull(String aliasName, Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNull";
        this.lastWhere.compareFun = new StampFieldFun("isNull", new StampColumn(aliasName, field));
        return this;
    }

    @Override
    public Object isNotNull(Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNotNull";
        this.lastWhere.compareFun = new StampFieldFun("not isNull", new StampColumn(field));
        return this;
    }

    @Override
    public Object isNotNull(Class table, Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNotNull";
        this.lastWhere.compareFun = new StampFieldFun("not isNull", new StampColumn(table, field));
        return this;
    }

    @Override
    public Object isNotNull(String aliasName, Serializable field) {
        this.gammars.add("operator");
        this.lastWhere.operator = "isNotNull";
        this.lastWhere.compareFun = new StampFieldFun("not isNull", new StampColumn(aliasName, field));
        return this;
    }

    @Override
    public Object eq() {
        this.gammars.add("operator");
        this.lastWhere.operator = "=";
        return this;
    }

    @Override
    public Object value(Object value) {
        this.gammars.add("value");
        this.lastWhere.value = value;
        return this;
    }

    @Override
    public Object in() {
        this.gammars.add("operator");
        this.lastWhere.operator = "in";
        return this;
    }

    @Override
    public Object nin() {
        this.gammars.add("operator");
        this.lastWhere.operator = "not in";
        return this;
    }

    @Override
    public Object like() {
        this.gammars.add("operator");
        this.lastWhere.operator = "like";
        return this;
    }

    @Override
    public Object ne() {
        this.gammars.add("operator");
        this.lastWhere.operator = "!=";
        return this;
    }

    @Override
    public Object gt() {
        this.gammars.add("operator");
        this.lastWhere.operator = ">";
        return this;
    }

    @Override
    public Object gte() {
        this.gammars.add("operator");
        this.lastWhere.operator = ">=";
        return this;
    }

    @Override
    public Object lt() {
        this.gammars.add("operator");
        this.lastWhere.operator = "<";
        return this;
    }

    @Override
    public Object lte() {
        this.gammars.add("operator");
        this.lastWhere.operator = "<=";
        return this;
    }

    @Override
    public BetweenValueBuilder between() {
        this.gammars.add("between");
        this.lastWhere.operator = "between";
        return this;
    }

    @Override
    public BetweenValueBuilder notBetween() {
        this.gammars.add("notBetween");
        this.lastWhere.operator = "not between";
        return this;
    }

    @Override
    public Object section(Object valueA, Object valueB) {
        this.gammars.add("section");
        this.lastWhere.value = valueA;
        this.lastWhere.value2 = valueB;
        return this;
    }

    @Override
    public Object or() {
        this.gammars.add("or");
        this.lastWhere.nextLogic = KeyLogic.OR;
        return this;
    }

    @Override
    public Object orderBy() {
        this.gammars.add("orderBy");
        return this;
    }

    @Override
    public Object asc() {
        this.gammars.add("asc");
        this.lastOrderBy.sortType = KeySortType.ASC;
        return this;
    }

    @Override
    public Object desc() {
        this.gammars.add("desc");
        this.lastOrderBy.sortType = KeySortType.DESC;
        return this;
    }

    @Override
    public Object where() {
        this.gammars.add("where");
        return this;
    }

    @Override
    public Object wrapper(AboutChildBuilder builder) {
        this.gammars.add("wrapper");

        return this;
    }

    @Override
    public Object inner() {
        this.gammars.add("inner");
        return this;
    }

    @Override
    public Object join() {
        this.gammars.add("join");
        if (this.previous("inner")) {
            this.lastJoin = new StampSelectJoin();
            this.lastJoin.joinType = KeyJoinType.INNER;
            this.joins.add(this.lastJoin);
        }
        if (this.previous("left")) {
            this.lastJoin = new StampSelectJoin();
            this.lastJoin.joinType = KeyJoinType.LEFT;
            this.joins.add(this.lastJoin);
        }
        return this;
    }

    @Override
    public Object left() {
        this.gammars.add("left");
        return this;
    }

    @Override
    public Object on() {
        this.gammars.add("on");
        if (this.hasPreviousStops(new String[]{"join"}, new String[]{"join"})) {
            this.lastWhere = new StampWhere();
            if (this.lastJoin.on == null) this.lastJoin.on = this.lastWhere;
        }
        return this;
    }

    @Override
    public Object groupBy() {
        this.gammars.add("groupBy");
        return this;
    }

    @Override
    public Object table(Class table) {
        this.gammars.add("table");
        if (this.hasPreviousStops(new String[]{"inner"}, new String[]{"join"})) {
            this.lastJoin.table = table;
        } else if (this.hasPreviousStops(new String[]{"left"}, new String[]{"join"})) {
            this.lastJoin.table = table;
        } else {
            this.stampFroms.add(new StampFrom(table));
        }
        return this;
    }

    @Override
    public Object table(Class table, String tableAliasName) {
        this.gammars.add("table");
        if (this.hasPreviousStops(new String[]{"inner"}, new String[]{"join"})) {
            this.lastJoin.table = table;
            this.lastJoin.tableAliasName = tableAliasName;
        } else if (this.hasPreviousStops(new String[]{"left"}, new String[]{"join"})) {
            this.lastJoin.table = table;
            this.lastJoin.tableAliasName = tableAliasName;
        } else {
            this.stampFroms.add(new StampFrom(table, tableAliasName));
        }
        return this;
    }

    @Override
    public Object as(String aliasName) {
        this.gammars.add("as");
        if (this.previous("fun")) {
            this.getLastField().aliasName = aliasName;
        }
        return this;
    }

    protected Object[] covertFunParam(Serializable... params) {
        if (params != null) {
            Object[] newParams = new Object[params.length];
            int i = 0;
            for (Serializable param : params) {
                if (param instanceof FieldItem) {
                    StampColumn column = new StampColumn();
                    column.table = ((FieldItem) param).getTable();
                    column.column = ((FieldItem) param).getField();
                    column.tableAliasName = ((FieldItem) param).getTableAliasName();
                    newParams[i] = column;
                } else {
                    newParams[i] = param;
                }
                i++;
            }
            return newParams;
        }
        return null;
    }

    @Override
    public Object count(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("COUNT", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object max(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("MAX", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object avg(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("AVG", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object sum(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("SUM", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object min(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("MIN", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object concat(Serializable... params) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("CONCAT", this.covertFunParam(params));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public Object substring(Serializable param, int pos, int len) {
        this.gammars.add("fun");
        StampSelectField field = new StampSelectField();
        field.fun = new StampFieldFun("SUBSTRING", this.covertFunParam(param, pos, len));
        this.stampSelectFields.add(field);
        return this;
    }

    @Override
    public StampAction compile() {
        if (stampSelectFields != null && stampSelectFields.size() > 0) {
            this.stampSelect.columns = stampSelectFields.toArray(new StampSelectField[]{});
        }
        if (stampFroms != null && stampFroms.size() > 0) {
            this.stampSelect.froms = this.stampFroms.toArray(new StampFrom[]{});
        }
        if (joins != null && joins.size() > 0) {
            this.stampSelect.joins = this.joins.toArray(new StampSelectJoin[]{});
        }
        if (this.where != null) {
            this.stampSelect.where = where;
        }
        if (this.having != null) {
            this.stampSelect.having = having;
        }
        if (this.orderBys != null && this.orderBys.size() > 0) {
            this.stampSelect.orderBy = this.orderBys.toArray(new StampOrderBy[]{});
        }
        if (this.groupBy != null && this.groupBy.size() > 0) {
            this.stampSelect.groupBy = this.groupBy.toArray(new StampColumn[]{});
        }
        return this.stampSelect;
    }
}