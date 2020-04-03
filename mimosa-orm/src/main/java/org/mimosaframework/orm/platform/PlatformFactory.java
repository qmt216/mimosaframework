package org.mimosaframework.orm.platform;

import org.mimosaframework.core.utils.i18n.Messages;
import org.mimosaframework.orm.MimosaDataSource;
import org.mimosaframework.orm.i18n.LanguageMessageFactory;
import org.mimosaframework.orm.platform.db2.*;
import org.mimosaframework.orm.platform.mysql.*;
import org.mimosaframework.orm.platform.oracle.*;
import org.mimosaframework.orm.platform.postgresql.*;
import org.mimosaframework.orm.platform.sqlite.*;
import org.mimosaframework.orm.platform.sqlserver.*;
import org.mimosaframework.orm.sql.stamp.*;
import org.mimosaframework.orm.utils.DatabaseTypes;

public class PlatformFactory {
    public static DifferentColumn getDifferentColumn(MimosaDataSource dataSource) {
        DatabaseTypes typeEnum = dataSource.getDatabaseTypeEnum();
        if (typeEnum.equals(DatabaseTypes.MYSQL)) {
            return new MysqlDifferentColumn();
        }
        if (typeEnum.equals(DatabaseTypes.ORACLE)) {
            return new OracleDifferentColumn();
        }
        if (typeEnum.equals(DatabaseTypes.SQL_SERVER)) {
            return new SQLServerDifferentColumn();
        }
        if (typeEnum.equals(DatabaseTypes.POSTGRESQL)) {
            return new PostgreSQLDifferentColumn();
        }
        if (typeEnum.equals(DatabaseTypes.DB2)) {
            return new DB2DifferentColumn();
        }
        if (typeEnum.equals(DatabaseTypes.SQLITE)) {
            return new SqliteDifferentColumn();
        }
        return null;
    }

    public static DatabasePorter getDatabasePorter(ActionDataSourceWrapper dswrapper) {
        MimosaDataSource dataSource = dswrapper.getDataSource();
        DatabaseTypes typeEnum = dataSource.getDatabaseTypeEnum();
        DatabasePorter databasePorter = null;
        if (typeEnum.equals(DatabaseTypes.MYSQL)) {
            databasePorter = new MysqlDatabasePorter();
        }
        if (typeEnum.equals(DatabaseTypes.ORACLE)) {
            databasePorter = new OracleDatabasePorter();
        }
        if (typeEnum.equals(DatabaseTypes.SQL_SERVER)) {
            databasePorter = new SQLServerDatabasePorter();
        }
        if (typeEnum.equals(DatabaseTypes.POSTGRESQL)) {
            databasePorter = new PostgreSQLDatabasePorter();
        }
        if (typeEnum.equals(DatabaseTypes.DB2)) {
            databasePorter = new DB2DatabasePorter();
        }
        if (typeEnum.equals(DatabaseTypes.SQLITE)) {
            databasePorter = new SqliteDatabasePorter();
        }
        if (databasePorter != null) {
            databasePorter.setCarryHandler(getCarryHandler(dswrapper));
        }
        return databasePorter;
    }

    public static CarryHandler getCarryHandler(ActionDataSourceWrapper dswrapper) {
        MimosaDataSource dataSource = dswrapper.getDataSource();
        DatabaseTypes typeEnum = dataSource.getDatabaseTypeEnum();
        if (typeEnum.equals(DatabaseTypes.MYSQL)) {
            return new MysqlCarryHandler(dswrapper);
        }
        if (typeEnum.equals(DatabaseTypes.ORACLE)) {
            return new OracleCarryHandler(dswrapper);
        }
        if (typeEnum.equals(DatabaseTypes.SQL_SERVER)) {
            return new SQLServerCarryHandler(dswrapper);
        }
        if (typeEnum.equals(DatabaseTypes.POSTGRESQL)) {
            return new PostgreSQLCarryHandler(dswrapper);
        }
        if (typeEnum.equals(DatabaseTypes.DB2)) {
            return new DB2CarryHandler(dswrapper);
        }
        if (typeEnum.equals(DatabaseTypes.SQLITE)) {
            return new SqliteCarryHandler(dswrapper);
        }
        return null;
    }

    public static PlatformWrapper getPlatformWrapper(ActionDataSourceWrapper dswrapper) {
        CarryHandler carryHandler = getCarryHandler(dswrapper);
        DatabasePorter databasePorter = getDatabasePorter(dswrapper);

        if (carryHandler == null || databasePorter == null) {
            throw new IllegalArgumentException(Messages.get(LanguageMessageFactory.PROJECT,
                    PlatformFactory.class, "not_support_platform"));
        }

        return new PlatformWrapperImpl(databasePorter, carryHandler);
    }

    public static ObjectSymbolContrast getSymbolContrast(MimosaDataSource dataSource) {
        return getSymbolContrast(dataSource.getDatabaseTypeEnum());
    }

    public static ObjectSymbolContrast getSymbolContrast(DatabaseTypes type) {
        if (type.equals(DatabaseTypes.MYSQL)) {
            return new MysqlObjectSymbolContrast();
        }
        if (type.equals(DatabaseTypes.ORACLE)) {
            return new OracleObjectSymbolContrast();
        }
        if (type.equals(DatabaseTypes.SQL_SERVER)) {
            return new SQLServerObjectSymbolContrast();
        }
        if (type.equals(DatabaseTypes.POSTGRESQL)) {
            return new PostgreSQLObjectSymbolContrast();
        }
        if (type.equals(DatabaseTypes.DB2)) {
            return new DB2ObjectSymbolContrast();
        }
        if (type.equals(DatabaseTypes.SQLITE)) {
            return new SqliteObjectSymbolContrast();
        }
        return null;
    }

    public static DatabaseSpeciality getLocalSpeciality(MimosaDataSource dataSource) {
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.MYSQL)) {
            return new MysqlDatabaseSpeciality();
        }
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.ORACLE)) {
            return new OracleDatabaseSpeciality();
        }
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.SQL_SERVER)) {
            return new SQLServerDatabaseSpeciality();
        }
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.POSTGRESQL)) {
            return new PostgreSQLDatabaseSpeciality();
        }
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.DB2)) {
            return new DB2DatabaseSpeciality();
        }
        if (dataSource.getDatabaseTypeEnum().equals(DatabaseTypes.SQLITE)) {
            return new SqliteDatabaseSpeciality();
        }
        return null;
    }

    public static StampCombineBuilder getStampAlterBuilder(DatabaseTypes databaseTypes, StampAction stampAction) {
        if (databaseTypes == DatabaseTypes.MYSQL) {
            if (stampAction instanceof StampAlter) return new MysqlStampAlter();
            if (stampAction instanceof StampCreate) return new MysqlStampCreate();
            if (stampAction instanceof StampDelete) return new MysqlStampDelete();
            if (stampAction instanceof StampDrop) return new MysqlStampDrop();
            if (stampAction instanceof StampInsert) return new MysqlStampInsert();
            if (stampAction instanceof StampSelect) return new MysqlStampSelect();
            if (stampAction instanceof StampUpdate) return new MysqlStampUpdate();
        } else if (databaseTypes == DatabaseTypes.ORACLE) {
            if (stampAction instanceof StampAlter) return new OracleStampAlter();
            if (stampAction instanceof StampCreate) return new OracleStampCreate();
            if (stampAction instanceof StampDelete) return new OracleStampDelete();
            if (stampAction instanceof StampDrop) return new OracleStampDrop();
            if (stampAction instanceof StampInsert) return new OracleStampInsert();
            if (stampAction instanceof StampSelect) return new OracleStampSelect();
            if (stampAction instanceof StampUpdate) return new OracleStampUpdate();
        }
        return null;
    }
}
