package org.mimosaframework.orm.platform.sqlserver;

import org.mimosaframework.orm.mapping.MappingTable;
import org.mimosaframework.orm.platform.DataDefinition;
import org.mimosaframework.orm.platform.PlatformDialect;
import org.mimosaframework.orm.platform.SQLBuilderCombine;
import org.mimosaframework.orm.platform.TableStructure;
import org.mimosaframework.orm.sql.stamp.*;

import java.sql.SQLException;

public class SQLServerPlatformDialect extends PlatformDialect {
    @Override
    public SQLBuilderCombine alter(StampAlter alter) {
        return null;
    }

    @Override
    public SQLBuilderCombine create(StampCreate create) {
        return null;
    }

    @Override
    public SQLBuilderCombine drop(StampDrop drop) {
        return null;
    }

    @Override
    public SQLBuilderCombine insert(StampInsert insert) {
        return null;
    }

    @Override
    public SQLBuilderCombine delete(StampDelete delete) {
        return null;
    }

    @Override
    public SQLBuilderCombine select(StampSelect select) {
        return null;
    }

    @Override
    public SQLBuilderCombine update(StampUpdate update) {
        return null;
    }

    @Override
    public boolean isSupportGeneratedKeys() {
        return false;
    }
}
