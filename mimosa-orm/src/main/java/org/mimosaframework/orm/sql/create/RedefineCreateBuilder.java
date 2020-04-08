package org.mimosaframework.orm.sql.create;

import org.mimosaframework.orm.sql.*;

public interface RedefineCreateBuilder
        extends
        CreateBuilder,
        DatabaseBuilder,
        INEBuilder,
        AbsNameBuilder,
        CharsetBuilder,
        CollateBuilder,
        TableBuilder,
        AbsColumnBuilder,
        AbsExtraBuilder,
        UniqueBuilder,
        IndexBuilder,
        OnBuilder,
        AbsTableBuilder,
        CreateIndexColumnsBuilder,
        AbsTableNameBuilder,
        ColumnTypeBuilder,
        ColumnAssistBuilder,
        CreateCommentForBuilder,
        KeyBuilder {
}
