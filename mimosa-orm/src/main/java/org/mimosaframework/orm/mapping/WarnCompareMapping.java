package org.mimosaframework.orm.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mimosaframework.orm.MimosaDataSource;
import org.mimosaframework.orm.convert.MappingNamedConvert;
import org.mimosaframework.orm.platform.ActionDataSourceWrapper;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WarnCompareMapping extends NothingCompareMapping {
    private static final Log logger = LogFactory.getLog(WarnCompareMapping.class);

    public WarnCompareMapping(ActionDataSourceWrapper dataSourceWrapper, Set<MappingTable> mappingTables) {
        super(dataSourceWrapper, mappingTables);
    }

    @Override
    public NotMatchObject doMapping() throws SQLException {
        NotMatchObject notMatchObject = super.doMapping();

        List<MappingTable> missingTables = notMatchObject.getMissingTables();
        List<MappingField> missingFields = notMatchObject.getMissingFields();
        List<MappingField> changeFields = notMatchObject.getChangeFields();

        if (missingTables != null) {
            for (MappingTable table : missingTables) {
                logger.warn("对比数据库发现缺失映射表 " + table.getMappingTableName() + " 映射类 " + table.getMappingClass().getSimpleName());
            }
        }

        if (missingFields != null) {
            for (MappingField field : missingFields) {
                MappingTable table = field.getMappingTable();
                if (table == null) {
                    logger.warn("对比数据库发现缺失字段 " + field.getMappingColumnName());
                } else {
                    logger.warn("对比数据库发现缺失字段 " + field.getMappingColumnName() + " 在表 " + table.getMappingTableName() + " 中");
                }
            }
        }

        if (changeFields != null) {
            for (MappingField field : changeFields) {
                MappingTable table = field.getMappingTable();
                if (table == null) {
                    logger.warn("对比数据库发现字段 " + field.getMappingColumnName() + " 已经被修改");
                } else {
                    logger.warn("对比数据库发现缺失字段 " + field.getMappingColumnName() + " 已经被修改," + "在表 " + table.getMappingTableName() + " 中");
                }
            }
        }

        return notMatchObject;
    }
}
