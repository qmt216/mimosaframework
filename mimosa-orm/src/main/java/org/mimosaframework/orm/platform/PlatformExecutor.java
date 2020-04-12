package org.mimosaframework.orm.platform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mimosaframework.core.utils.StringTools;
import org.mimosaframework.orm.i18n.I18n;
import org.mimosaframework.orm.mapping.MappingField;
import org.mimosaframework.orm.mapping.MappingGlobalWrapper;
import org.mimosaframework.orm.mapping.MappingIndex;
import org.mimosaframework.orm.mapping.MappingTable;
import org.mimosaframework.orm.sql.stamp.*;

import java.sql.SQLException;
import java.util.*;

/**
 * 执行数据库数据结构校验
 * 以及CURD操作
 */
public class PlatformExecutor {
    private static final Log logger = LogFactory.getLog(PlatformExecutor.class);

    public void compareTableStructure(MappingGlobalWrapper mapping,
                                      DataSourceWrapper dswrapper,
                                      PlatformCompare compare) throws SQLException {
        PlatformDialect dialect = this.getDialect(mapping, dswrapper);
        List<TableStructure> structures = dialect.getTableStructures();
        List<MappingTable> mappingTables = mapping.getMappingTables();

        if (structures != null) {
            List<MappingTable> rmTab = new ArrayList<>();
            for (TableStructure structure : structures) {
                List<TableColumnStructure> columnStructures = new ArrayList<>(structure.getColumnStructures());

                MappingTable currTable = null;
                for (MappingTable mappingTable : mappingTables) {
                    String mappingTableName = mappingTable.getMappingTableName();
                    String tableName = structure.getTableName();
                    if (tableName.equalsIgnoreCase(mappingTableName)) {
                        rmTab.add(mappingTable);
                        currTable = mappingTable;
                        break;
                    }
                }

                if (currTable != null) {
                    List<MappingField> rmCol = new ArrayList<>();
                    List<TableColumnStructure> rmSCol = new ArrayList<>();
                    Set<MappingField> mappingFields = currTable.getMappingFields();
                    if (columnStructures != null && columnStructures.size() > 0) {
                        Map<MappingField, List<ColumnEditType>> updateFields = new LinkedHashMap();
                        Map<MappingField, TableColumnStructure> updateColumnsStructures = new LinkedHashMap();
                        for (TableColumnStructure columnStructure : columnStructures) {
                            if (mappingFields != null) {
                                MappingField currField = null;
                                for (MappingField field : mappingFields) {
                                    String mappingFieldName = field.getMappingColumnName();
                                    String fieldName = columnStructure.getColumnName();
                                    if (mappingFieldName.equalsIgnoreCase(fieldName)) {
                                        currField = field;
                                        rmCol.add(field);
                                        rmSCol.add(columnStructure);
                                        break;
                                    }
                                }

                                if (currField != null) {
                                    List<ColumnEditType> columnEditTypes = new ArrayList<>();
                                    ColumnType columnType = dialect.getColumnType(JavaType2ColumnType.getColumnTypeByJava(currField.getMappingFieldType()));
                                    if (columnType == null) {
                                        throw new IllegalArgumentException(I18n.print("platform_executor_empty_type", currField.getMappingFieldType().getSimpleName()));
                                    }
                                    if (!columnStructure.getTypeName().equalsIgnoreCase(columnType.getTypeName())
                                            || columnStructure.getLength() != currField.getMappingFieldLength()
                                            || columnStructure.getScale() != currField.getDatabaseColumnDecimalDigits()) {
                                        columnEditTypes.add(ColumnEditType.TYPE);
                                    }

                                    boolean nullable = columnStructure.isNullable();
                                    boolean isPk = structure.isPrimaryKeyColumn(columnStructure.getColumnName());
                                    if (!isPk && nullable != currField.isMappingFieldNullable()) {
                                        columnEditTypes.add(ColumnEditType.ISNULL);
                                    }

                                    String defA = currField.getMappingFieldDefaultValue();
                                    String defB = columnStructure.getDefaultValue();
                                    if ((StringTools.isNotEmpty(defA) && !defA.equals(defB)) || (StringTools.isNotEmpty(defB) && !defB.equals(defA))) {
                                        columnEditTypes.add(ColumnEditType.DEF_VALUE);
                                    }
                                    if (columnStructure.isAutoIncrement() != currField.isMappingAutoIncrement()) {
                                        columnEditTypes.add(ColumnEditType.AUTO_INCREMENT);
                                    }

                                    String cmtA = currField.getMappingFieldComment();
                                    String cmtB = columnStructure.getComment();
                                    if ((StringTools.isNotEmpty(cmtA) && !cmtA.equals(cmtB)) || (StringTools.isNotEmpty(cmtB) && !cmtB.equals(cmtA))) {
                                        columnEditTypes.add(ColumnEditType.COMMENT);
                                    }
                                    if (currField.isMappingFieldPrimaryKey() != isPk) {
                                        columnEditTypes.add(ColumnEditType.PRIMARY_KEY);
                                    }

                                    if (columnEditTypes.size() > 0) {
                                        // 需要修改字段
                                        updateFields.put(currField, columnEditTypes);
                                        updateColumnsStructures.put(currField, columnStructure);
                                    }
                                }
                            }
                        }

                        if (updateFields != null && updateFields.size() > 0) {
                            compare.fieldUpdate(mapping, currTable, structure, updateFields, updateColumnsStructures);
                        }

                        mappingFields.removeAll(rmCol);
                        columnStructures.removeAll(rmSCol);
                        if (mappingFields.size() > 0) {
                            // 有新添加的字段需要添加
                            compare.fieldAdd(mapping, currTable, structure, new ArrayList<MappingField>(mappingFields));
                        }
                        if (columnStructures.size() > 0) {
                            // 有多余的字段需要删除
                            compare.fieldDel(mapping, currTable, structure, columnStructures);
                        }
                    } else {
                        // 数据库的字段没有需要重新添加全部字段
                        compare.fieldAdd(mapping, currTable, structure, new ArrayList<MappingField>(mappingFields));
                    }
                }

                if (currTable != null) {
                    Set<MappingIndex> mappingIndexes = currTable.getMappingIndexes();
                    if (mappingIndexes != null) {
                        List<MappingIndex> newIndexes = new ArrayList<>();
                        List<MappingIndex> updateIndexes = new ArrayList<>();
                        List<String> updateIndexNames = new ArrayList<>();
                        for (MappingIndex index : mappingIndexes) {
                            String mappingIndexName = index.getIndexName();
                            List<TableIndexStructure> indexStructures = structure.getIndexStructures(mappingIndexName);

                            if (indexStructures != null && indexStructures.size() > 0) {
                                List<MappingField> indexMappingFields = index.getIndexColumns();
                                if (!indexStructures.get(0).getType().equalsIgnoreCase(index.getIndexType().toString())) {
                                    // 索引类型不一致需要重建索引
                                    updateIndexes.add(index);
                                    updateIndexNames.add(mappingIndexName);
                                } else {
                                    List<MappingField> rmIdxCol = new ArrayList<>();
                                    for (TableIndexStructure indexStructure : indexStructures) {
                                        String indexColumnName = indexStructure.getColumnName();
                                        for (MappingField indexMappingField : indexMappingFields) {
                                            if (indexMappingField.getMappingColumnName().equalsIgnoreCase(indexColumnName)) {
                                                rmIdxCol.add(indexMappingField);
                                            }
                                        }
                                    }
                                    indexMappingFields.removeAll(rmIdxCol);
                                    if (indexMappingFields.size() != 0) {
                                        // 需要重建索引
                                        updateIndexes.add(index);
                                        updateIndexNames.add(mappingIndexName);
                                    }
                                }
                            } else {
                                // 需要新建索引
                                newIndexes.add(index);
                            }
                        }
                        if (updateIndexes != null && updateIndexes.size() > 0) {
                            compare.indexUpdate(mapping, currTable, updateIndexes, updateIndexNames);
                        }
                        if (newIndexes != null && newIndexes.size() > 0) {
                            compare.indexAdd(mapping, currTable, newIndexes);
                        }
                    }
                }
            }
            mappingTables.removeAll(rmTab);
            if (mappingTables.size() != 0) {
                // 映射表没有添加到数据库
                // 需要新建数据库表

                for (MappingTable mappingTable : mappingTables) {
                    Set<MappingIndex> mappingIndex = mappingTable.getMappingIndexes();
                    compare.tableCreate(mapping, mappingTable);
                    compare.indexAdd(mapping, mappingTable, new ArrayList<MappingIndex>(mappingIndex));
                }
            }
        }
    }

    private PlatformDialect getDialect(MappingGlobalWrapper mappingGlobalWrapper,
                                       DataSourceWrapper dswrapper) {
        PlatformDialect dialect = PlatformFactory.getDialect(dswrapper);
        dialect.setMappingGlobalWrapper(mappingGlobalWrapper);
        return dialect;
    }

    public void createTable(MappingGlobalWrapper mappingGlobalWrapper,
                            DataSourceWrapper dswrapper,
                            MappingTable mappingTable) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.CREATE_TABLE, mappingTable));
    }

    public void dropTable(MappingGlobalWrapper mappingGlobalWrapper,
                          DataSourceWrapper dswrapper,
                          TableStructure tableStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.DROP_TABLE, tableStructure));
    }

    public void createField(MappingGlobalWrapper mappingGlobalWrapper,
                            DataSourceWrapper dswrapper,
                            MappingTable mappingTable,
                            TableStructure tableStructure,
                            MappingField mappingField) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.ADD_COLUMN, mappingTable, tableStructure, mappingField));
    }

    public void modifyField(MappingGlobalWrapper mappingGlobalWrapper,
                            DataSourceWrapper dswrapper,
                            MappingTable mappingTable,
                            TableStructure tableStructure,
                            MappingField mappingField,
                            TableColumnStructure columnStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.MODIFY_COLUMN, tableStructure, mappingTable, mappingField, columnStructure));
    }

    public void dropField(MappingGlobalWrapper mappingGlobalWrapper,
                          DataSourceWrapper dswrapper,
                          MappingTable mappingTable,
                          TableStructure tableStructure,
                          TableColumnStructure columnStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.DROP_COLUMN, mappingTable, tableStructure, columnStructure));
    }

    public void createIndex(MappingGlobalWrapper mappingGlobalWrapper,
                            DataSourceWrapper dswrapper,
                            MappingTable mappingTable,
                            MappingIndex mappingIndex) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.ADD_INDEX, mappingTable, mappingIndex));
    }

    public void dropIndex(MappingGlobalWrapper mappingGlobalWrapper,
                          DataSourceWrapper dswrapper,
                          MappingTable mappingTable,
                          String indexName) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        dialect.define(new DataDefinition(DataDefinitionType.DROP_INDEX, mappingTable, indexName));
    }

    public Object dialect(MappingGlobalWrapper mappingGlobalWrapper,
                          DataSourceWrapper dswrapper,
                          StampAction stampAction) throws SQLException {
        PlatformDialect dialect = this.getDialect(mappingGlobalWrapper, dswrapper);
        SQLBuilderCombine combine = null;
        if (stampAction instanceof StampAlter) {
            combine = dialect.alter((StampAlter) stampAction);
        }
        if (stampAction instanceof StampCreate) {
            combine = dialect.create((StampCreate) stampAction);
        }
        if (stampAction instanceof StampDrop) {
            combine = dialect.drop((StampDrop) stampAction);
        }
        if (stampAction instanceof StampInsert) {
            combine = dialect.insert((StampInsert) stampAction);
        }
        if (stampAction instanceof StampDelete) {
            combine = dialect.delete((StampDelete) stampAction);
        }
        if (stampAction instanceof StampSelect) {
            combine = dialect.select((StampSelect) stampAction);
        }
        if (stampAction instanceof StampUpdate) {
            combine = dialect.update((StampUpdate) stampAction);
        }

        if (combine != null) {
            return new DefaultDBRunner(dswrapper).doHandler(new JDBCTraversing(combine.getSql(), combine.getPlaceholders()));
        }
        return null;
    }


}
