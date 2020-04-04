package org.mimosaframework.orm.platform.oracle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mimosaframework.core.utils.StringTools;
import org.mimosaframework.core.utils.i18n.Messages;
import org.mimosaframework.orm.i18n.LanguageMessageFactory;
import org.mimosaframework.orm.mapping.MappingGlobalWrapper;
import org.mimosaframework.orm.platform.SQLBuilderCombine;
import org.mimosaframework.orm.sql.stamp.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OracleStampAlter extends OracleStampCommonality implements StampCombineBuilder {
    private static final Log logger = LogFactory.getLog(OracleStampAlter.class);
    protected int totalAction = 0;
    protected boolean noNeedSource = false;

    @Override
    public SQLBuilderCombine getSqlBuilder(MappingGlobalWrapper wrapper,
                                           StampAction action) {
        StampAlter alter = (StampAlter) action;
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER");
        if (alter.target == KeyTarget.DATABASE) {
            sb.append(" DATABASE");

            sb.append(" " + RS + alter.name + RE);

            if (StringTools.isNotEmpty(alter.charset)) {
                sb.append(" DEFAULT CHARACTER SET " + alter.charset);
            }
            if (StringTools.isNotEmpty(alter.collate)) {
                sb.append(" COLLATE " + alter.collate);
            }
        }
        if (alter.target == KeyTarget.TABLE) {
            sb.append(" TABLE");

            sb.append(" " + this.getTableName(wrapper, alter.table, alter.name));

            if (alter.items != null) {
                for (StampAlterItem item : alter.items) {
                    this.buildAlterItem(wrapper, sb, alter, item);
                }
            } else {
                // oracle 没有修改表字符集的设置
                sb = null;
                logger.warn("oracle can't set table charset");
            }
        }

        if (totalAction <= 1 && noNeedSource) sb = null;
        return new SQLBuilderCombine(this.toSQLString(sb), null);
    }

    private void buildAlterItem(MappingGlobalWrapper wrapper,
                                StringBuilder sb,
                                StampAlter alter,
                                StampAlterItem item) {
        if (item.action == KeyAction.ADD) {
            totalAction++;
            sb.append(" ADD");
            if (item.struct == KeyAlterStruct.COLUMN) {
                this.buildAlterColumn(sb, wrapper, alter, item, false);
            }
            if (item.struct == KeyAlterStruct.INDEX) {
                this.buildAlterIndex(sb, wrapper, alter, item);
            }
        }

        if (item.action == KeyAction.CHANGE) {
            totalAction++;
            sb.append(" MODIFY");
            String oldColumnName = this.getColumnName(wrapper, alter, item.oldColumn);
            String newColumnName = this.getColumnName(wrapper, alter, item.column);
            if (!oldColumnName.equalsIgnoreCase(newColumnName)) {
                StringBuilder rnsb = new StringBuilder();
                rnsb.append("ALTER TABLE");
                rnsb.append(" " + this.getTableName(wrapper, alter.table, alter.name));
                rnsb.append(" RENAME COLUMN " + oldColumnName + " TO " + newColumnName);
                this.getBuilders().add(new ExecuteImmediate(rnsb));
            }
            this.buildAlterColumn(sb, wrapper, alter, item, true);
        }

        if (item.action == KeyAction.MODIFY) {
            totalAction++;
            sb.append(" MODIFY");
            this.buildAlterColumn(sb, wrapper, alter, item, false);
        }

        if (item.action == KeyAction.DROP) {
            totalAction++;
            sb.append(" DROP");
            if (item.dropType == KeyAlterDropType.COLUMN) {
                sb.append(" COLUMN");
                sb.append(" " + this.getColumnName(wrapper, alter, item.column));
            }
            if (item.dropType == KeyAlterDropType.INDEX) {
                sb.setLength(0);
                sb.append("DROP");
                sb.append(" INDEX");
                sb.append(" " + RS + item.name + RE);
            }
            if (item.dropType == KeyAlterDropType.PRIMARY_KEY) {
                sb.append(" PRIMARY KEY");
            }
        }

        if (item.action == KeyAction.RENAME) {
            totalAction++;
            sb.append(" RENAME");
            if (item.renameType == KeyAlterRenameType.COLUMN) {
                sb.append(" COLUMN");
                sb.append(" " + this.getColumnName(wrapper, alter, item.oldColumn));
                sb.append(" TO");
                sb.append(" " + this.getColumnName(wrapper, alter, item.column));
            }
            if (item.renameType == KeyAlterRenameType.INDEX) {
                sb.append(" INDEX");
                sb.append(" " + item.oldName);
                sb.append(" TO");
                sb.append(" " + item.name);
            }
            if (item.renameType == KeyAlterRenameType.TABLE) {
                sb.append(" " + item.name);
            }
        }

        if (item.action == KeyAction.AUTO_INCREMENT) {
            totalAction++;
            this.noNeedSource = true;
            String tableName = this.getTableName(wrapper, alter.table, alter.name);
            String seqName = tableName + "_SEQ";

            this.getDeclares().add("CACHE_CUR_SEQ NUMBER");
            this.getBuilders().add(new ExecuteImmediate().setProcedure("BEGIN"));
            this.getBuilders().add(new ExecuteImmediate().setProcedure("SELECT " + seqName + ".NEXTVAL INTO CACHE_CUR_SEQ FROM DUAL"));
            this.getBuilders().add(new ExecuteImmediate().setProcedure("EXCEPTION WHEN NO_DATA_FOUND THEN CACHE_CUR_SEQ:=0"));
            this.getBuilders().add(new ExecuteImmediate().setProcedure("END"));
            this.getBuilders().add(new ExecuteImmediate().setProcedure("EXECUTE IMMEDIATE concat('ALTER SEQUENCE " +
                    seqName + " INCREMENT BY '," + item.value + "-CACHE_CUR_SEQ)"));
            this.getBuilders().add(new ExecuteImmediate().setProcedure("SELECT " + seqName + ".NEXTVAL INTO CACHE_CUR_SEQ FROM DUAL"));
            this.getBuilders().add(new ExecuteImmediate("ALTER SEQUENCE " + seqName + " INCREMENT BY 1"));
        }
        if (item.action == KeyAction.CHARACTER_SET) {
            totalAction++;
            sb.append(" CHARACTER SET = " + item.name);
        }
        if (item.action == KeyAction.COMMENT) {
            totalAction++;
            this.addCommentSQL(wrapper, alter, item, item.comment, 1);
        }
    }

    private void buildAlterIndex(StringBuilder sb,
                                 MappingGlobalWrapper wrapper,
                                 StampAlter alter,
                                 StampAlterItem item) {
        if (item.indexType != KeyIndexType.PRIMARY_KEY) {
            sb.setLength(0);
            sb.append("CREATE");
        }
        if (item.indexType == KeyIndexType.FULLTEXT) {
            sb.append(" INDEX");
            logger.warn("oracle not support fulltext index , please manually create");
        } else if (item.indexType == KeyIndexType.UNIQUE) {
            sb.append(" UNIQUE");
            sb.append(" INDEX");
        } else if (item.indexType == KeyIndexType.PRIMARY_KEY) {
            sb.append(" PRIMARY KEY");
        } else {
            sb.append(" INDEX");
        }

        if (StringTools.isNotEmpty(item.name)) {
            sb.append(" " + RS + item.name + RE);
        }
        if (item.indexType != KeyIndexType.PRIMARY_KEY) {
            sb.append(" ON ");
            sb.append(this.getTableName(wrapper, alter.table, alter.name));
        }

        List<String> fullTextIndexNames = new ArrayList<>();

        if (item.columns != null && item.columns.length > 0) {
            for (StampColumn column : item.columns) {
                fullTextIndexNames.add(this.getColumnName(wrapper, alter, column, false));
            }
            sb.append(" (");
            if (item.indexType == KeyIndexType.FULLTEXT) {
                sb.append(this.getColumnName(wrapper, alter, item.columns[0]));
            } else {
                int i = 0;
                for (StampColumn column : item.columns) {
                    sb.append(this.getColumnName(wrapper, alter, column));
                    i++;
                    if (i != item.columns.length) {
                        sb.append(",");
                    }
                }
            }
            sb.append(")");
        } else {
            throw new IllegalArgumentException(Messages.get(LanguageMessageFactory.PROJECT,
                    StampAction.class, "miss_index_columns"));
        }

//        if (item.indexType == KeyIndexType.FULLTEXT) {
//            // this.getBegins().add(new ExecuteImmediate().setProcedure("CTX_DDL.DROP_PREFERENCE('MIMOSA_LEXER')"));
//            this.getDeclares().add("HAS_PREFERENCE NUMBER");
//            this.getBegins().add(new ExecuteImmediate().setProcedure("BEGIN"));
//            this.getBegins().add(new ExecuteImmediate().setProcedure("SELECT 1 INTO HAS_PREFERENCE FROM CTX_PARAMETERS WHERE PAR_VALUE = 'MIMOSA_LEXER'"));
//            this.getBegins().add(new ExecuteImmediate().setProcedure("EXCEPTION WHEN NO_DATA_FOUND THEN HAS_PREFERENCE:=0"));
//            this.getBegins().add(new ExecuteImmediate().setProcedure("END"));
//            this.getBegins().add(new ExecuteImmediate().setProcedure("IF (HAS_PREFERENCE!=1) THEN CTX_DDL.CREATE_PREFERENCE('MIMOSA_LEXER','CHINESE_VGRAM_LEXER');END IF"));
//            if (item.columns != null && item.columns.length > 1) {
//                String iallName = this.getTableName(wrapper, alter.table, alter.name) + "_";
//                String cls = "";
//                Iterator<String> iterator = fullTextIndexNames.iterator();
//                while (iterator.hasNext()) {
//                    String s = iterator.next();
//                    iallName += s;
//                    cls += s;
//                    if (iterator.hasNext()) {
//                        iallName += "_";
//                        cls += ",";
//                    }
//                }
//                iallName = iallName.toUpperCase();
//                // this.getBegins().add(new ExecuteImmediate().setProcedure("CTX_DDL.DROP_PREFERENCE('" + iallName + "')"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("BEGIN"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("SELECT 1 INTO HAS_PREFERENCE FROM CTX_PARAMETERS WHERE PAR_VALUE = '" + iallName + "'"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("EXCEPTION WHEN NO_DATA_FOUND THEN HAS_PREFERENCE:=0"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("END"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("IF (HAS_PREFERENCE!=1) THEN CTX_DDL.CREATE_PREFERENCE('"
//                        + iallName + "','MULTI_COLUMN_DATASTORE');END IF"));
//                this.getBegins().add(new ExecuteImmediate().setProcedure("CTX_DDL.SET_ATTRIBUTE('" + iallName + "','COLUMNS','" + cls + "')"));
//                sb.append(" INDEXTYPE IS CTXSYS.CONTEXT PARAMETERS(''DATASTORE " + iallName + " LEXER MIMOSA_LEXER'')");
//            } else {
//                sb.append(" INDEXTYPE IS CTXSYS.CONTEXT PARAMETERS(''LEXER MIMOSA_LEXER'')");
//            }
//        }

        // oracle 没有所以注释 common on
        if (StringTools.isNotEmpty(item.comment)) {
            logger.warn("oracle have no index comment");
        }
    }

    private void buildAlterColumn(StringBuilder sb,
                                  MappingGlobalWrapper wrapper,
                                  StampAlter alter,
                                  StampAlterItem column,
                                  boolean isOldColumn) {
        sb.append(" " + this.getColumnName(wrapper, alter, isOldColumn ? column.oldColumn : column.column));
        if (column.columnType != null) {
            sb.append(" " + this.getColumnType(column.columnType, column.len, column.scale));
        }
        if (!column.nullable) {
            sb.append(" NOT NULL");
        }
        if (column.autoIncrement) {
            this.addAutoIncrement(wrapper, alter);
        }
        if (column.pk) {
            sb.append(" PRIMARY KEY");
        }
        if (column.unique) {
            sb.append(" UNIQUE");
        }
        if (column.key) {
            sb.append(" KEY");
        }
        if (StringTools.isNotEmpty(column.defaultValue)) {
            sb.append(" DEFAULT \"" + column.defaultValue + "\"");
        }
        if (StringTools.isNotEmpty(column.comment)) {
            this.addCommentSQL(wrapper, alter, column, column.comment, 1);
        }
        if (column.after != null) {
            sb.append(" AFTER " + this.getColumnName(wrapper, alter, column.after));
        }
        if (column.before != null) {
            sb.append(" BEFORE " + this.getColumnName(wrapper, alter, column.before));
        }
    }

    protected void addCommentSQL(MappingGlobalWrapper wrapper,
                                 StampAlter alter,
                                 StampAlterItem item,
                                 String commentStr,
                                 int type) {
        List<StampAction.STItem> items = alter.getTables();
        if (items != null && items.size() > 0) {
            StringBuilder comment = new StringBuilder();
            if (type == 1) {
                StampColumn column = item.column;
                comment.append("COMMENT ON COLUMN ");
                if (column != null) column.table = items.get(0).getTable();
                comment.append(this.getColumnName(wrapper, alter, column));
            }
            if (type == 2) {
                comment.append("COMMENT ON INDEX ");
                comment.append(this.getTableName(wrapper, items.get(0).getTable(), null));
                comment.append("." + RS + item.name + RE);
            }
            comment.append(" IS ");
            comment.append("''" + commentStr + "''");
            this.getBuilders().add(new ExecuteImmediate(comment));
        }
    }

    protected void addAutoIncrement(MappingGlobalWrapper wrapper,
                                    StampAlter alter) {
        String tableName = this.getTableName(wrapper, alter.table, alter.name);
        String seqName = tableName + "_SEQ";
        this.getDeclares().add("SEQUENCE_COUNT NUMBER");
        this.getBuilders().add(new ExecuteImmediate().setProcedure("BEGIN"));
        this.getBuilders().add(new ExecuteImmediate().setProcedure("SELECT 1 INTO SEQUENCE_COUNT FROM user_sequences WHERE sequence_name = '" + seqName + "'"));
        this.getBegins().add(new ExecuteImmediate().setProcedure("EXCEPTION WHEN NO_DATA_FOUND THEN SEQUENCE_COUNT:=0"));
        this.getBegins().add(new ExecuteImmediate().setProcedure("END"));
        this.getBuilders().add(new ExecuteImmediate("IF (SEQUENCE_COUNT!=1) THEN ",
                "CREATE SEQUENCE " + seqName + " INCREMENT BY 1 START WITH 1 MINVALUE 1 MAXVALUE 9999999999999999", "END IF"));
    }
}