package org.mimosaframework.orm.mapping;

import org.mimosaframework.orm.platform.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class UpdateCompareMapping extends AddCompareMapping {

    public UpdateCompareMapping(ActionDataSourceWrapper dataSourceWrapper, Set<MappingTable> mappingTables) {
        super(dataSourceWrapper, mappingTables);
    }

    @Override
    public NotMatchObject doMapping() throws SQLException {
        NotMatchObject notMatchObject = super.doMapping();

        ActionDataSourceWrapper wrapper = dataSourceWrapper.newDataSourceWrapper();
        wrapper.setAutoCloseConnection(true);
        DatabasePorter porter = PlatformFactory.getDatabasePorter(wrapper.getDataSource());
        CarryHandler carryHandler = PlatformFactory.getCarryHandler(wrapper);

        List<MappingField> changeFields = notMatchObject.getChangeFields();

        if (changeFields != null) {
            for (MappingField field : changeFields) {
                // 这里必须保证MappingField的mappingTable存在
                PorterStructure[] structures = porter.updateField(field);
                // 使用SQL语句添加字段
                if (structures != null) {
                    carryHandler.doHandler(structures);
                }
            }
        }
        return notMatchObject;
    }
}
