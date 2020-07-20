package org.mimosaframework.orm.platform;

import org.mimosaframework.orm.mapping.MappingGlobalWrapper;
import org.mimosaframework.orm.sql.stamp.StampColumn;
import org.mimosaframework.orm.sql.stamp.StampFormula;
import org.mimosaframework.orm.sql.stamp.StampUpdate;
import org.mimosaframework.orm.sql.stamp.StampUpdateItem;

import java.util.Iterator;
import java.util.List;

public abstract class PlatformStampUpdate extends PlatformStampCommonality {
    public PlatformStampUpdate(PlatformStampSection section, PlatformStampReference reference, PlatformDialect dialect, PlatformStampShare share) {
        super(section, reference, dialect, share);
    }

    protected void buildUpdateItem(MappingGlobalWrapper wrapper,
                                   StampUpdate update,
                                   StampUpdateItem item,
                                   StringBuilder sb,
                                   List<SQLDataPlaceholder> placeholders) {
        item.column.table = null;
        item.column.tableAliasName = null;
        String name = this.reference.getColumnName(wrapper, update, item.column);
        sb.append(name);
        sb.append(" = ");

        if (item.value instanceof StampColumn) {
            StampColumn column = (StampColumn) item.value;
            column.table = null;
            column.tableAliasName = null;
            sb.append(this.reference.getColumnName(wrapper, update, column));
        } else if (item.value instanceof StampFormula) {
            StampFormula formula = (StampFormula) item.value;
            if (formula != null && formula.formulas != null) {
                StampFormula.Formula[] formulas = formula.formulas;
                boolean hasFirst = false;
                for (int i = 0; i < formulas.length; i++) {
                    StampFormula.Formula fml = formulas[i];
                    if (fml.column != null || fml.value != null) {
                        if (hasFirst && fml.express != null) {
                            if (fml.express == StampFormula.Express.ADD) sb.append(" + ");
                            if (fml.express == StampFormula.Express.MINUS) sb.append(" - ");
                        }
                        if (fml.column != null) {
                            sb.append(this.reference.getColumnName(wrapper, update, fml.column));
                        } else {
                            if (fml.value instanceof Number) {
                                Number fmlValue = (Number) fml.value;
                                if (fml.value instanceof Float) sb.append(fmlValue.floatValue());
                                else if (fml.value instanceof Double) sb.append(fmlValue.doubleValue());
                                else if (fml.value instanceof Integer) sb.append(fmlValue.intValue());
                                else if (fml.value instanceof Long) sb.append(fmlValue.longValue());
                                else sb.append(fmlValue.intValue());
                            } else if (fml.value instanceof String) {
                                sb.append(fml.value);
                            }
                        }
                        hasFirst = true;
                    }
                }
            }
        } else {
            sb.append("?");
            placeholders.add(new SQLDataPlaceholder(name, item.value));
        }
    }
}