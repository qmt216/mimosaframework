package org.mimosaframework.orm.platform.oracle;

import org.mimosaframework.core.utils.StringTools;
import org.mimosaframework.orm.mapping.MappingField;
import org.mimosaframework.orm.mapping.MappingGlobalWrapper;
import org.mimosaframework.orm.mapping.MappingTable;
import org.mimosaframework.orm.platform.SQLDataPlaceholder;
import org.mimosaframework.orm.sql.stamp.*;

import java.util.ArrayList;
import java.util.List;

public abstract class OracleStampCommonality {
    protected static final String RS = "\"";
    protected static final String RE = "\"";
    protected String NL = "\n";
    protected String TAB = "\t";
    protected String NL_TAB = "\n\t";

    protected List<String> declares = null;
    protected List<ExecuteImmediate> builders = null;
    protected List<ExecuteImmediate> begins = null;

    class ExecuteImmediate {
        public String procedure;

        public String preview;
        public String sql;
        public String end;

        public ExecuteImmediate() {
        }

        public ExecuteImmediate(String sql) {
            this.sql = sql;
        }

        public ExecuteImmediate(StringBuilder sql) {
            this.sql = sql.toString();
        }

        public ExecuteImmediate(String preview, String sql) {
            this.preview = preview;
            this.sql = sql;
        }

        public ExecuteImmediate(String preview, String sql, String end) {
            this.preview = preview;
            this.sql = sql;
            this.end = end;
        }

        public ExecuteImmediate setProcedure(String procedure) {
            this.procedure = procedure;
            return this;
        }
    }

    protected List<ExecuteImmediate> getBuilders() {
        if (builders == null) builders = new ArrayList<>();
        return builders;
    }

    protected List<ExecuteImmediate> getBegins() {
        if (begins == null) begins = new ArrayList<>();
        return begins;
    }

    protected List<String> getDeclares() {
        if (declares == null) declares = new ArrayList<>();
        return declares;
    }

    protected String toSQLString(StringBuilder sb) {
        if (this.builders != null || this.begins != null) {
            if (sb != null) {
                if (this.builders == null) this.builders = new ArrayList<>();
                this.builders.add(0, new ExecuteImmediate(sb.toString()));
            }
            StringBuilder nsb = new StringBuilder();
            if (declares != null && declares.size() > 0) {
                nsb.append(NL + "DECLARE ");
                for (String s : declares) {
                    nsb.append(NL_TAB + s + ";");
                }
            }
            nsb.append(NL + "BEGIN ");
            this.appendBuilders(nsb, begins);
            this.appendBuilders(nsb, builders);
            nsb.append(NL + "END;");
            return nsb.toString();
        } else {
            return sb != null ? sb.toString() : null;
        }
    }

    private void appendBuilders(StringBuilder nsb, List<ExecuteImmediate> builders) {
        if (builders != null) {
            for (ExecuteImmediate item : builders) {
                if (StringTools.isNotEmpty(item.procedure)) {
                    if (item.procedure.equalsIgnoreCase("BEGIN")) {
                        nsb.append(NL_TAB + item.procedure);
                    } else {
                        nsb.append(NL_TAB + item.procedure + ";");
                    }
                } else {
                    if (StringTools.isNotEmpty(item.preview)) {
                        nsb.append(NL_TAB + item.preview + " EXECUTE IMMEDIATE ");
                    } else {
                        nsb.append(NL_TAB + "EXECUTE IMMEDIATE ");
                    }

                    if (StringTools.isNotEmpty(item.end)) {
                        nsb.append("'" + item.sql + "';");
                        nsb.append(item.end + ";");
                    } else {
                        nsb.append("'" + item.sql + "'; ");
                    }
                }
            }
        }
    }

    protected String getTableName(MappingGlobalWrapper wrapper,
                                  Class table,
                                  String tableName) {
        if (table != null) {
            MappingTable mappingTable = wrapper.getMappingTable(table);
            if (mappingTable != null) {
                return mappingTable.getMappingTableName().toUpperCase();
            } else if (StringTools.isNotEmpty(tableName)) {
                return tableName.toUpperCase();
            }
        } else if (StringTools.isNotEmpty(tableName)) {
            return tableName.toUpperCase();
        }
        return null;
    }

    protected String getColumnName(MappingGlobalWrapper wrapper, StampAction stampTables, StampColumn column) {
        return this.getColumnName(wrapper, stampTables, column, true);
    }

    /**
     * oracle 的列名不区分大小写，但是如果被双引号引用的则区分大小写
     * 比如
     * id 和 ID 一致
     * "id" 和 "ID" 不一致
     * <p>
     * 如果列名不加双引号则创建表时数据库自动大写,加了双引号则为原始字符串
     *
     * @param wrapper
     * @param stampTables
     * @param column
     * @return
     */
    protected String getColumnName(MappingGlobalWrapper wrapper,
                                   StampAction stampTables,
                                   StampColumn column,
                                   boolean hasRes) {
        String RS = this.RS, RE = this.RE;
        if (!hasRes) {
            RS = "";
            RE = "";
        }
        if (column != null && column.column != null) {
            String columnName = column.column.toString();
            String tableAliasName = column.tableAliasName;

            if (columnName.equals("*")) {
                if (StringTools.isNotEmpty(tableAliasName)) {
                    return tableAliasName.toUpperCase() + "." + columnName;
                } else {
                    return columnName;
                }
            }

            List<StampAction.STItem> tables = stampTables.getTables();
            if (tables != null && StringTools.isNotEmpty(tableAliasName)) {
                for (StampAction.STItem stItem : tables) {
                    if (tableAliasName.equals(stItem.getTableAliasName())) {
                        MappingTable mappingTable = wrapper.getMappingTable(stItem.getTable());
                        if (mappingTable != null) {
                            MappingField mappingField = mappingTable.getMappingFieldByName(columnName);
                            if (mappingField != null) {
                                return tableAliasName.toUpperCase() + "." + RS + mappingField.getMappingColumnName() + RE;
                            }
                        }
                    }
                }
            }

            if (column.table != null) {
                MappingTable mappingTable = wrapper.getMappingTable(column.table);
                if (mappingTable != null) {
                    MappingField mappingField = mappingTable.getMappingFieldByName(columnName);
                    if (mappingField != null) {
                        return mappingTable.getMappingTableName().toUpperCase()
                                + "."
                                + RS + mappingField.getMappingColumnName() + RE;
                    } else {
                        return mappingTable.getMappingTableName().toUpperCase()
                                + "."
                                + RS + columnName + RE;
                    }
                }
            }

            if (tables != null) {
                for (StampAction.STItem stItem : tables) {
                    MappingTable mappingTable = wrapper.getMappingTable(stItem.getTable());
                    if (mappingTable != null) {
                        MappingField mappingField = mappingTable.getMappingFieldByName(columnName);
                        if (mappingField != null) {
                            return RS + mappingField.getMappingColumnName() + RE;
                        }
                    }
                }
            }

            return RS + columnName + RE;
        }
        return null;
    }

    protected void buildWhere(MappingGlobalWrapper wrapper,
                              List<SQLDataPlaceholder> placeholders,
                              StampAction stampTables,
                              StampWhere where,
                              StringBuilder sb) {
        KeyWhereType whereType = where.whereType;
        StampWhere next = where.next;

        if (whereType == KeyWhereType.WRAP) {
            StampWhere wrapWhere = where.wrapWhere;
            sb.append("(");
            this.buildWhere(wrapper, placeholders, stampTables, wrapWhere, sb);
            sb.append(")");
        } else {
            StampFieldFun fun = where.fun;
            StampColumn leftColumn = where.leftColumn;
            StampFieldFun leftFun = where.leftFun;
            Object leftValue = where.leftValue;
            StampColumn rightColumn = where.rightColumn;
            StampFieldFun rightFun = where.rightFun;
            Object rightValue = where.rightValue;
            Object rightValueEnd = where.rightValueEnd;

            String key = null;
            if (whereType == KeyWhereType.NORMAL) {
                if (leftColumn != null) {
                    key = this.getColumnName(wrapper, stampTables, leftColumn);
                    sb.append(key);
                } else if (leftFun != null) {
                    this.buildSelectFieldFun(wrapper, stampTables, leftFun, sb);
                    key = leftFun.funName;
                } else if (leftValue != null) {
                    sb.append("?");

                    SQLDataPlaceholder placeholder = new SQLDataPlaceholder();
                    placeholder.setName("Unknown");
                    placeholder.setValue(leftValue);
                    placeholders.add(placeholder);
                }

                if (where.not) sb.append(" NOT");
                sb.append(" " + where.operator + " ");

                if (rightColumn != null) {
                    String columnName = this.getColumnName(wrapper, stampTables, rightColumn);
                    sb.append(columnName);
                } else if (rightFun != null) {
                    this.buildSelectFieldFun(wrapper, stampTables, rightFun, sb);
                } else if (rightValue != null) {
                    sb.append("?");

                    SQLDataPlaceholder placeholder = new SQLDataPlaceholder();
                    placeholder.setName(key);
                    placeholder.setValue(rightValue);
                    placeholders.add(placeholder);
                }
            }
            if (whereType == KeyWhereType.KEY_AND) {
                if (leftColumn != null) {
                    key = this.getColumnName(wrapper, stampTables, leftColumn);
                    sb.append(key);
                } else if (leftFun != null) {
                    this.buildSelectFieldFun(wrapper, stampTables, leftFun, sb);
                    key = leftFun.funName;
                } else if (leftValue != null) {
                    sb.append("?");

                    SQLDataPlaceholder placeholder = new SQLDataPlaceholder();
                    placeholder.setName("Unknown");
                    placeholder.setValue(leftValue);
                    placeholders.add(placeholder);
                }
                if (where.not) sb.append(" NOT");
                sb.append(" " + where.operator + " ");

                sb.append("?");

                SQLDataPlaceholder placeholder1 = new SQLDataPlaceholder();
                if (StringTools.isEmpty(key)) {
                    placeholder1.setName("Unknown&Start");
                } else {
                    placeholder1.setName(key + "&Start");
                }
                placeholder1.setValue(rightValue);
                placeholders.add(placeholder1);

                sb.append(" AND ");

                sb.append("?");

                SQLDataPlaceholder placeholder2 = new SQLDataPlaceholder();
                if (StringTools.isEmpty(key)) {
                    placeholder2.setName("Unknown&End");
                } else {
                    placeholder1.setName(key + "&End");
                }
                placeholder2.setValue(rightValueEnd);
                placeholders.add(placeholder2);
            }

            if (whereType == KeyWhereType.FUN) {
                if (where.not) sb.append("NOT ");
                this.buildSelectFieldFun(wrapper, stampTables, fun, sb);
            }
        }

        if (next != null) {
            if (where.nextLogic == KeyLogic.AND)
                sb.append(" AND ");
            else if (where.nextLogic == KeyLogic.OR)
                sb.append(" OR ");

            this.buildWhere(wrapper, placeholders, stampTables, next, sb);
        }
    }

    protected void buildSelectFieldFun(MappingGlobalWrapper wrapper,
                                       StampAction stampTables,
                                       StampFieldFun fun,
                                       StringBuilder sb) {
        String funName = fun.funName.toUpperCase();
        Object[] params = fun.params;

        sb.append(funName);
        if (params != null) {
            sb.append("(");
            for (Object param : params) {
                if (param instanceof StampColumn) {
                    sb.append(this.getColumnName(wrapper, stampTables, (StampColumn) param));
                }
                if (param instanceof StampKeyword) {
                    if (((StampKeyword) param).distinct) sb.append("DISTINCT ");
                }
                if (param instanceof Number) {
                    sb.append(param);
                }
                if (param instanceof String) {
                    sb.append(param);
                }
                if (param instanceof StampFieldFun) {
                    this.buildSelectFieldFun(wrapper, stampTables,
                            (StampFieldFun) param, sb);
                }
            }
            sb.append(")");
        }
    }

    /**
     * ORACLE的数据类型 -- ORACLE的数据类型
     * 常用的数据库字段类型如下：
     * 字段类型 中文说明 限制条件 其它说明
     * CHAR 固定长度字符串 最大长度2000 bytes
     * VARCHAR2 可变长度的字符串 最大长度4000 bytes 可做索引的最大长度749
     * NCHAR 根据字符集而定的固定长度字符串 最大长度2000 bytes
     * NVARCHAR2 根据字符集而定的可变长度字符串 最大长度4000 bytes
     * DATE 日期（日-月-年） DD-MM-YY（HH-MI-SS） 无千虫问题
     * LONG 超长字符串 最大长度2G（231-1） 足够存储大部头著作
     * RAW 固定长度的二进制数据 最大长度2000 bytes 可存放多媒体图象声音等
     * LONG RAW 可变长度的二进制数据 最大长度2G 同上
     * BLOB 二进制数据 最大长度4G
     * CLOB 字符数据 最大长度4G
     * NCLOB 根据字符集而定的字符数据 最大长度4G
     * BFILE 存放在数据库外的二进制数据 最大长度4G
     * ROWID 数据表中记录的唯一行号 10 bytes ********.****.****格式，*为0或1
     * NROWID 二进制数据表中记录的唯一行号 最大长度4000 bytes
     * NUMBER(P,S) 数字类型 P为总位数，S为小数位数
     * DECIMAL(P,S) 数字类型 P为总位数，S为小数位数
     * INTEGER 整数类型 小的整数
     * FLOAT 浮点数类型 NUMBER(38)，双精度
     * REAL 实数类型 NUMBER(63)，精度更高
     *
     * @param columnType
     * @param len
     * @param scale
     * @return
     */
    protected String getColumnType(KeyColumnType columnType, int len, int scale) {
        if (columnType == KeyColumnType.INT) {
            return "INT";
        }
        if (columnType == KeyColumnType.VARCHAR) {
            return "VARCHAR2(" + len + ")";
        }
        if (columnType == KeyColumnType.CHAR) {
            return "CHAR(" + len + ")";
        }
        if (columnType == KeyColumnType.BLOB) {
            return "BLOB";
        }
        if (columnType == KeyColumnType.TEXT) {
            return "TEXT";
        }
        if (columnType == KeyColumnType.TINYINT) {
            return "TINYINT";
        }
        if (columnType == KeyColumnType.SMALLINT) {
            return "SMALLINT";
        }
        if (columnType == KeyColumnType.MEDIUMINT) {
            return "MEDIUMINT";
        }
        if (columnType == KeyColumnType.BIT) {
            return "BIT";
        }
        if (columnType == KeyColumnType.BIGINT) {
            return "BIGINT";
        }
        if (columnType == KeyColumnType.FLOAT) {
            return "FLOAT";
        }
        if (columnType == KeyColumnType.DOUBLE) {
            return "DOUBLE";
        }
        if (columnType == KeyColumnType.DECIMAL) {
            return "DECIMAL(" + len + "," + scale + ")";
        }
        if (columnType == KeyColumnType.BOOLEAN) {
            return "BOOLEAN";
        }
        if (columnType == KeyColumnType.DATE) {
            return "DATE";
        }
        if (columnType == KeyColumnType.TIME) {
            return "TIME";
        }
        if (columnType == KeyColumnType.DATETIME) {
            return "DATE";
        }
        if (columnType == KeyColumnType.TIMESTAMP) {
            return "TIMESTAMP";
        }
        if (columnType == KeyColumnType.YEAR) {
            return "YEAR";
        }
        return null;
    }
}