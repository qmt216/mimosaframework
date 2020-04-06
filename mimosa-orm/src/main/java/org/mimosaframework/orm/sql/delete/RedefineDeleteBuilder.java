package org.mimosaframework.orm.sql.delete;

import org.mimosaframework.orm.sql.*;

public interface RedefineDeleteBuilder
        extends
        DeleteBuilder,
        AbsTableBuilder,
        FromBuilder,
        WhereBuilder,
        AbsWhereColumnBuilder,
        AbsWhereValueBuilder,
        OperatorBuilder,
        OperatorFunctionBuilder,
        AbsValueBuilder,
        BetweenValueBuilder,
        LogicBuilder,
        WrapperBuilder,
        SortBuilder {
}
