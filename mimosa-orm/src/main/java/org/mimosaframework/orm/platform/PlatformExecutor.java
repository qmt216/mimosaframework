package org.mimosaframework.orm.platform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mimosaframework.core.json.ModelObject;
import org.mimosaframework.core.utils.StringTools;
import org.mimosaframework.orm.BasicFunction;
import org.mimosaframework.orm.ModelObjectConvertKey;
import org.mimosaframework.orm.criteria.*;
import org.mimosaframework.orm.i18n.I18n;
import org.mimosaframework.orm.mapping.MappingField;
import org.mimosaframework.orm.mapping.MappingGlobalWrapper;
import org.mimosaframework.orm.mapping.MappingIndex;
import org.mimosaframework.orm.mapping.MappingTable;
import org.mimosaframework.orm.merge.DefaultObjectMerge;
import org.mimosaframework.orm.merge.MergeTree;
import org.mimosaframework.orm.merge.ObjectMerge;
import org.mimosaframework.orm.sql.*;
import org.mimosaframework.orm.sql.delete.DefaultSQLDeleteBuilder;
import org.mimosaframework.orm.sql.insert.DefaultSQLInsertBuilder;
import org.mimosaframework.orm.sql.select.DefaultSQLSelectBuilder;
import org.mimosaframework.orm.sql.stamp.*;
import org.mimosaframework.orm.sql.update.DefaultSQLUpdateBuilder;
import org.mimosaframework.orm.transaction.Transaction;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行数据库数据结构校验
 * 以及CURD操作
 */
public class PlatformExecutor {
    private static final Log logger = LogFactory.getLog(PlatformExecutor.class);
    private DBRunner runner;
    private MappingGlobalWrapper mappingGlobalWrapper;
    private SessionContext sessionContext;

    public PlatformExecutor(MappingGlobalWrapper mappingGlobalWrapper,
                            SessionContext sessionContext) {
        this.mappingGlobalWrapper = mappingGlobalWrapper;
        this.sessionContext = sessionContext;
        this.runner = new DefaultDBRunner(sessionContext);
    }

    public void compareTableStructure(PlatformCompare compare) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        List<String> existTableNames = new ArrayList<>();
        List<MappingTable> mappingTables = mappingGlobalWrapper.getMappingTables();
        if (mappingTables != null && mappingTables.size() > 0) {
            for (MappingTable mappingTable : mappingTables) {
                existTableNames.add(mappingTable.getMappingTableName().toLowerCase());
            }
        }
        List<TableStructure> structures = dialect.getTableStructures(existTableNames);

        if (structures != null) {
            List<MappingTable> rmTab = new ArrayList<>();
            for (TableStructure structure : structures) {
                if (structure.getColumnStructures() != null) {
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

                    compare.start(currTable);
                    Map<MappingField, CompareUpdateMate> updateFields = new LinkedHashMap();
                    List<MappingField> createFields = new ArrayList<>();
                    List<TableColumnStructure> delColumns = new ArrayList<>();
                    List<MappingIndex> updateIndexes = new ArrayList<>();
                    List<MappingIndex> newIndexes = new ArrayList<>();
                    List<Map.Entry<String, List<TableIndexStructure>>> dropIndexes = new ArrayList<>();

                    if (currTable != null) {
                        List<MappingField> rmCol = new ArrayList<>();
                        List<TableColumnStructure> rmSCol = new ArrayList<>();
                        Set<MappingField> mappingFields = currTable.getMappingFields();
                        if (columnStructures != null && columnStructures.size() > 0) {
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
                                        List<ColumnEditType> columnEditTypes = dialect.compareColumnChange(
                                                structure, currField, columnStructure
                                        );

                                        if (columnEditTypes.size() > 0) {
                                            updateFields.put(currField, new CompareUpdateMate(columnEditTypes, columnStructure));
                                        }
                                    }
                                }
                            }

                            mappingFields.removeAll(rmCol);
                            columnStructures.removeAll(rmSCol);
                            if (mappingFields.size() > 0) {
                                // 有新添加的字段需要添加
                                createFields.addAll(mappingFields);
                            }
                            if (columnStructures.size() > 0) {
                                // 有多余的字段需要删除
                                delColumns.addAll(columnStructures);
                            }
                        } else {
                            // 数据库的字段没有需要重新添加全部字段
                            createFields.addAll(mappingFields);
                        }
                    }

                    if (currTable != null) {
                        Set<MappingIndex> mappingIndexes = currTable.getMappingIndexes();
                        Map<String, List<TableIndexStructure>> fromIndexStructure = structure.getMapIndex();
                        if (mappingIndexes != null) {
                            for (MappingIndex index : mappingIndexes) {
                                String mappingIndexName = index.getIndexName();
                                List<TableIndexStructure> indexStructures = (fromIndexStructure != null ? fromIndexStructure.get(mappingIndexName) : null);

                                if (indexStructures != null && indexStructures.size() > 0) {
                                    fromIndexStructure.remove(mappingIndexName);

                                    List<MappingField> indexMappingFields = index.getIndexColumns();
                                    if (!indexStructures.get(0).getType().equalsIgnoreCase(index.getIndexType().toString())) {
                                        // 索引类型不一致需要重建索引
                                        updateIndexes.add(index);
                                    } else {
                                        Set<MappingField> rmIdxCol = new HashSet<>();
                                        for (TableIndexStructure indexStructure : indexStructures) {
                                            String indexColumnName = indexStructure.getColumnName();
                                            for (MappingField indexMappingField : indexMappingFields) {
                                                if (indexMappingField.getMappingColumnName().equalsIgnoreCase(indexColumnName)) {
                                                    rmIdxCol.add(indexMappingField);
                                                }
                                            }
                                        }
                                        if (indexMappingFields.size() != rmIdxCol.size()) {
                                            // 需要重建索引
                                            updateIndexes.add(index);
                                        }
                                    }
                                } else {
                                    // 如果数据库支持同列不同名索引则直接添加
                                    if (dialect.isSupportSameColumnIndex()) {
                                        newIndexes.add(index);
                                    } else {
                                        List<TableIndexStructure> indexStructuresByColumns = structure.getIndexStructures(index.getIndexColumns());
                                        if (indexStructuresByColumns != null && indexStructuresByColumns.size() > 0) {
                                            if (!indexStructuresByColumns.get(0).getType().equalsIgnoreCase(index.getIndexType().toString())) {
                                                // 已经有一个相同类型的所有存在，不再做任何操作
                                            } else {
                                                // 已经有一个相同类型的所有存在，不再做任何操作
                                            }
                                        } else {
                                            // 需要新建索引
                                            newIndexes.add(index);
                                        }
                                    }
                                }
                            }
                        }

                        if (fromIndexStructure != null && fromIndexStructure.size() > 0) {
                            Set<MappingField> mappingFields = currTable.getMappingFields();
                            for (MappingField mappingField : mappingFields) {
                                if (mappingField.isMappingFieldUnique() || mappingField.isMappingFieldIndex() || mappingField.isMappingFieldPrimaryKey()) {
                                    Map.Entry<String, List<TableIndexStructure>> hasIndex = structure
                                            .getIndexStructures(fromIndexStructure, Arrays.asList(new MappingField[]{mappingField}));
                                    if (hasIndex != null && hasIndex.getValue() != null && hasIndex.getValue().size() == 1) {
                                        fromIndexStructure.remove(hasIndex.getKey());
                                    }
                                }
                            }

                            Iterator<Map.Entry<String, List<TableIndexStructure>>> iterator = fromIndexStructure.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, List<TableIndexStructure>> entry = iterator.next();
                                List<TableIndexStructure> values = entry.getValue();
                                if (values != null && values.size() > 0 && !values.get(0).getType().equalsIgnoreCase("P")) {
                                    dropIndexes.add(entry);
                                }
                            }
                        }
                    }


                    boolean update = false;
                    CompareUpdateTableMate tableMate = new CompareUpdateTableMate();
                    if (updateFields != null && updateFields.size() > 0) {
                        tableMate.setUpdateFields(updateFields);
                        update = true;
                    }
                    if (createFields != null && createFields.size() > 0) {
                        tableMate.setCreateFields(createFields);
                        update = true;
                    }
                    if (delColumns != null && delColumns.size() > 0) {
                        tableMate.setDelColumns(delColumns);
                        // 如果字段删除了那么对应的索引也被删除
                        // 所以去除已经被删除的字段索引
                        // 如果不删除已经删除的字段的索引，删除索引时会报不存在索引
                        if (dropIndexes != null && dropIndexes.size() > 0) {
                            List<Map.Entry<String, List<TableIndexStructure>>> rms = null;
                            for (Map.Entry<String, List<TableIndexStructure>> entry : dropIndexes) {
                                List<TableIndexStructure> iss = entry.getValue();
                                if (iss != null) {
                                    for (TableIndexStructure is : iss) {
                                        for (TableColumnStructure delC : delColumns) {
                                            if (StringTools.isNotEmpty(is.getColumnName())
                                                    && is.getColumnName().equals(delC.getColumnName())) {
                                                if (rms == null) rms = new ArrayList<>();
                                                rms.add(entry);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            if (rms != null) {
                                dropIndexes.removeAll(rms);
                            }
                        }
                        update = true;
                    }
                    if (updateIndexes != null && updateIndexes.size() > 0) {
                        tableMate.setUpdateIndexes(updateIndexes);
                        update = true;
                    }

                    if (newIndexes != null && newIndexes.size() > 0) {
                        if (!dialect.isSupportSameColumnIndex()) {
                            // 如果不支持多列索引则这里删除相同列索引
                            List<MappingIndex> onlyDiffIndex = new ArrayList<>();
                            for (MappingIndex index : newIndexes) {
                                boolean is = false;
                                for (MappingIndex odi : onlyDiffIndex) {
                                    if (index.isSameColumn(odi)) {
                                        is = true;
                                        break;
                                    }
                                }
                                if (!is) {
                                    onlyDiffIndex.add(index);
                                }
                            }
                            newIndexes = onlyDiffIndex;
                        }
                        tableMate.setNewIndexes(newIndexes);
                        update = true;
                    }

                    if (dropIndexes != null && dropIndexes.size() > 0) {
                        List<String> deli = new ArrayList<>();
                        for (Map.Entry<String, List<TableIndexStructure>> entry : dropIndexes) {
                            if (!structure.isIndexInConstraintByColumns(entry.getKey(), entry.getValue())) {
                                deli.add(entry.getKey());
                            } else {

                            }
                        }
                        tableMate.setDropIndexes(deli);
                        update = true;
                    }

                    if (update && currTable != null && structure != null) {
                        tableMate.setMappingTable(currTable);
                        tableMate.setStructure(structure);
                        tableMate.setTableStructures(structures);
                        tableMate.setDialect(dialect);
                        compare.checking(tableMate);
                    }
                }
            }
            mappingTables.removeAll(rmTab);
            if (mappingTables.size() != 0) {
                // 映射表没有添加到数据库
                // 需要新建数据库表

                for (MappingTable mappingTable : mappingTables) {
                    compare.start(mappingTable);
                    Set<MappingIndex> mappingIndex = mappingTable.getMappingIndexes();
                    CompareUpdateTableMate tableMate = new CompareUpdateTableMate();
                    if (mappingIndex != null && mappingIndex.size() > 0) {
                        tableMate.setNewIndexes(new ArrayList(mappingIndex));
                    }
                    tableMate.setCreateTable(mappingTable);
                    tableMate.setMappingTable(mappingTable);
                    compare.checking(tableMate);
                }
            }
        }
    }

    public PlatformDialect getDialect() {
        PlatformDialect dialect = PlatformFactory.getDialect(sessionContext);
        dialect.setMappingGlobalWrapper(this.mappingGlobalWrapper);
        return dialect;
    }

    public DialectNextStep createTable(MappingTable mappingTable) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.CREATE_TABLE, mappingTable));
    }

    public DialectNextStep dropTable(TableStructure tableStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.DROP_TABLE, tableStructure));
    }

    public DialectNextStep createField(MappingTable mappingTable,
                                       TableStructure tableStructure,
                                       MappingField mappingField) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.ADD_COLUMN, mappingTable, tableStructure, mappingField));
    }

    public DialectNextStep modifyField(MappingTable mappingTable,
                                       TableStructure tableStructure,
                                       MappingField mappingField,
                                       TableColumnStructure columnStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.MODIFY_COLUMN, tableStructure, mappingTable, mappingField, columnStructure));
    }

    public DialectNextStep dropField(MappingTable mappingTable, TableStructure tableStructure,
                                     TableColumnStructure columnStructure) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.DROP_COLUMN, mappingTable, tableStructure, columnStructure));
    }

    public DialectNextStep createIndex(MappingTable mappingTable, MappingIndex mappingIndex) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.ADD_INDEX, mappingTable, mappingIndex));
    }

    public DialectNextStep dropIndex(MappingTable mappingTable, String indexName) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        return dialect.define(new DataDefinition(DataDefinitionType.DROP_INDEX, mappingTable, indexName));
    }

    public void doDialectRebuild(List<TableStructure> structures,
                                 MappingTable mappingTable,
                                 TableStructure structure) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        dialect.rebuildTable(structures, mappingTable, structure);
    }

    public List<Long> inserts(MappingTable table, List<ModelObject> objects, boolean update) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        if (objects != null && objects.size() > 0) {
            Set<MappingField> fields = table.getMappingFields();
            DefaultSQLInsertBuilder insertBuilder = new DefaultSQLInsertBuilder();
            insertBuilder.insert().into().table(table.getMappingTableName());
            String[] columns = null;
            Set<Object> keys = null;
            int i = 0;
            if (objects.size() == 1) {
                ModelObject object = objects.get(0);
                keys = object.keySet();
                columns = new String[keys.size()];
                for (Object key : keys) {
                    MappingField mappingField = table.getMappingFieldByJavaName(String.valueOf(key));
                    if (mappingField == null) {
                        throw new IllegalArgumentException(I18n
                                .print("miss_executor_mapping_field", String.valueOf(key)));
                    }
                    columns[i] = mappingField.getMappingColumnName();
                    i++;
                }
            } else {
                keys = new LinkedHashSet<>();
                Set<String> objectKeys = new HashSet<>();
                for (ModelObject object : objects) {
                    Set sets = object.keySet();
                    objectKeys.addAll(sets);
                }

                columns = new String[objectKeys.size()];
                i = 0;
                for (MappingField mappingField : fields) {
                    String javaName = mappingField.getMappingFieldName();
                    String columnName = mappingField.getMappingColumnName();
                    if (objectKeys.contains(javaName)) {
                        keys.add(javaName);
                        columns[i] = columnName;
                        i++;
                    }
                }
            }
            insertBuilder.columns(columns).values();
            for (ModelObject object : objects) {
                Object[] values = new Object[keys.size()];
                i = 0;
                for (Object key : keys) {
                    values[i] = object.get(key);
                    i++;
                }
                insertBuilder.row(values);
            }

            StampInsert insert = insertBuilder.compile();
            if (insert != null && !dialect.isSupportGeneratedKeys()) {
                List<MappingField> pks = table.getMappingPrimaryKeyFields();
                for (MappingField pk : pks) {
                    if (pk.isMappingAutoIncrement()) {
                        insert.autoField = new StampInsert.StampInsertSequence();
                        insert.autoField.columnName = pk.getMappingColumnName();
                        break;
                    }
                }
            }
            SQLBuilderCombine combine = null;
            if (update) {
                combine = dialect.save(insert);
            } else {
                combine = dialect.insert(insert);
            }
            Object object = this.runner.doHandler(new JDBCTraversing(TypeForRunner.INSERT,
                    combine.getSql(), combine.getPlaceholders()));
            if (!dialect.isSupportGeneratedKeys()) {
                StampColumn[] stampColumns = insert.columns;
                Object[][] values = insert.values;
                int k = 0;
                for (StampColumn column : stampColumns) {
                    if (column.column.equals(insert.autoField.columnName)) {
                        break;
                    }
                    k++;
                }

                List<Long> ids = new ArrayList<>();
                for (Object[] o : values) {
                    ids.add((Long) o[k]);
                }
                return ids;
            }
            return (List<Long>) object;
        }
        return null;
    }

    public Integer update(MappingTable table, DefaultUpdate update) throws SQLException {
        PlatformDialect dialect = this.getDialect();

        Wraps<Filter> wraps = update.getLogicWraps();
        Map<Object, Object> sets = update.getValues();

        DefaultSQLUpdateBuilder updateBuilder = new DefaultSQLUpdateBuilder();
        updateBuilder.update().table(table.getMappingTableName());
        Iterator<Map.Entry<Object, Object>> iterator = sets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            Object key = entry.getKey();
            MappingField field = table.getMappingFieldByJavaName(String.valueOf(key));
            if (field != null) {
                Object value = entry.getValue();
                String fieldName = field.getMappingColumnName();
                if (value instanceof UpdateSetValue) {
                    StampFormula formula = new StampFormula();
                    StampFormula.Formula f1 = new StampFormula.Formula();
                    f1.column = new StampColumn(fieldName);
                    StampFormula.Formula f2 = new StampFormula.Formula();
                    if (((UpdateSetValue) value).getType() == UpdateSpecialType.ADD_SELF) {
                        f2.express = StampFormula.Express.ADD;
                    } else if (((UpdateSetValue) value).getType() == UpdateSpecialType.SUB_SELF) {
                        f2.express = StampFormula.Express.MINUS;
                    } else {
                        f2.express = StampFormula.Express.ADD;
                    }
                    f2.value = ((UpdateSetValue) value).getStep();
                    formula.formulas = new StampFormula.Formula[]{f1, f2};
                    updateBuilder.set(fieldName, formula);
                } else {
                    updateBuilder.set(fieldName, value);
                }
            }
        }

        if (wraps != null) updateBuilder.where();
        this.buildWraps(updateBuilder, table, wraps, false, null);
        SQLBuilderCombine combine = dialect.update(updateBuilder.compile());
        return (Integer) this.runner.doHandler(new JDBCTraversing(TypeForRunner.UPDATE,
                combine.getSql(), combine.getPlaceholders()));
    }

    public boolean isSupportDuplicateKeyUpdate() {
        PlatformDialect dialect = this.getDialect();
        return dialect.isSupportDuplicateKeyUpdate();
    }

    public Integer delete(MappingTable table, DefaultDelete delete) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        Wraps<Filter> wraps = delete.getLogicWraps();
        DefaultSQLDeleteBuilder deleteBuilder = new DefaultSQLDeleteBuilder();
        deleteBuilder.delete().from().table(table.getMappingTableName());

        if (wraps != null) deleteBuilder.where();
        this.buildWraps(deleteBuilder, table, wraps, false, null);
        SQLBuilderCombine combine = dialect.delete(deleteBuilder.compile());
        return (Integer) this.runner.doHandler(new JDBCTraversing(TypeForRunner.DELETE,
                combine.getSql(), combine.getPlaceholders()));
    }

    public List<ModelObject> select(DefaultQuery query, ModelObjectConvertKey convert) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        Wraps<Filter> logicWraps = query.getLogicWraps();
        Set<Join> joins = query.getJoins();
        Set<OrderBy> orders = query.getOrderBy();

        Map<Class, List<String>> fields = query.getFields();
        Map<Class, List<String>> excludes = query.getExcludes();

        Limit limit = query.getLimit();
        Class<?> tableClass = query.getTableClass();
        boolean isMaster = query.isMaster();
        String slaveName = query.getSlaveName();


        MappingTable queryTable = this.mappingGlobalWrapper.getMappingTable(tableClass);
        if ((orders == null || orders.size() == 0) && (
                (limit != null && dialect.isSelectLimitMustOrderBy()) || query.isWithoutOrderBy() == false)) {
            // 第一种情况  如果排序是空的且分页查询必须排序字段则默认添加主键升序
            // 第二种情况  如果没有排序则添加一个默认的排序
            List<MappingField> pks = queryTable.getMappingPrimaryKeyFields();
            if (orders == null) orders = new LinkedHashSet<>();
            if (pks != null && pks.size() > 0) {
                for (MappingField field : pks) {
                    orders.add(new OrderBy(true, field.getMappingFieldName()));
                }
            }
        }

        sessionContext.setMaster(isMaster);
        sessionContext.setSlaveName(slaveName);

        Map<Object, String> alias = new HashMap<>();
        alias.put(query, query.getQueryTableAs());
        Map<Object, List<SelectFieldAliasReference>> fieldAlias = null;
        int j = 1;
        AtomicInteger i = new AtomicInteger(1);
        boolean hasJoin = false;
        if (joins != null && joins.size() > 0) {
            hasJoin = true;
            alias.put(query, query.getQueryTableAs());
            MappingTable mappingTable = this.mappingGlobalWrapper.getMappingTable(tableClass);
            Set<MappingField> mappingFields = this.getSelectFields(fields, excludes, mappingTable);
            if (fieldAlias == null) fieldAlias = new HashMap<>();
            List<SelectFieldAliasReference> fieldAliasList = new ArrayList<>();
            for (MappingField field : mappingFields) {
                SelectFieldAliasReference reference = new SelectFieldAliasReference();
                reference.setFieldName(field.getMappingColumnName());
                reference.setFieldAliasName("F" + j);
                reference.setJavaFieldName(field.getMappingFieldName());
                reference.setTableAliasName(query.getQueryTableAs());
                reference.setTableClass(tableClass);
                reference.setMainClass(tableClass);
                reference.setPrimaryKey(field.isMappingFieldPrimaryKey());
                fieldAliasList.add(reference);

                j++;
            }
            fieldAlias.put(query, fieldAliasList);

            for (Join join : joins) {
                DefaultJoin defaultJoin = (DefaultJoin) join;
                Class table = defaultJoin.getTable();
                mappingTable = this.mappingGlobalWrapper.getMappingTable(table);
                if (fieldAlias == null) fieldAlias = new HashMap<>();
                String joinAliasName = getJoinAlias(alias, join, i);
                alias.put(join, joinAliasName);

                mappingFields = this.getSelectFields(fields, excludes, mappingTable);
                fieldAliasList = new ArrayList<>();
                for (MappingField field : mappingFields) {
                    SelectFieldAliasReference reference = new SelectFieldAliasReference();
                    reference.setFieldName(field.getMappingColumnName());
                    reference.setFieldAliasName("F" + j);
                    reference.setJavaFieldName(field.getMappingFieldName());
                    reference.setTableAliasName(joinAliasName);
                    reference.setTableClass(table);
                    reference.setMainClass(defaultJoin.getMainClass());
                    reference.setPrimaryKey(field.isMappingFieldPrimaryKey());
                    fieldAliasList.add(reference);
                    j++;
                }
                fieldAlias.put(join, fieldAliasList);
            }
        }

        // 判断是否需要组合子查询，以解决join和limit的数量问题
        boolean isChildSelect = false;
        if (limit != null && joins != null && joins.size() > 0) {
            isChildSelect = true;
        }

        DefaultSQLSelectBuilder select = new DefaultSQLSelectBuilder();
        select.select();

        if (isChildSelect) {
            select.distinctByAlias(query.getQueryTableAs(), "*");
        } else {
            if (hasJoin) {
                this.buildSelectField(select, alias, fieldAlias);
            } else {
                if (fields != null || excludes != null) {
                    Set<MappingField> mappingFields = this.getSelectFields(fields, excludes, queryTable);
                    for (MappingField field : mappingFields) {
                        select.fields(query.getQueryTableAs(), field.getMappingColumnName());
                    }
                } else {
                    select.all();
                }
            }
        }
        select.from().table(queryTable.getMappingTableName(), query.getQueryTableAs());
        // 如果是子查询语句，则子查询语句只需要包含inner join的联合条件
        this.buildJoinQuery(query, select, alias, joins, isChildSelect ? true : false);
        if (logicWraps != null) select.where();
        this.buildWraps(select, queryTable, logicWraps, hasJoin, query);

        this.buildOrderBy(query, select, null, orders, null,
                queryTable, joins != null && joins.size() > 0);

        if (limit != null) {
            select.limit(limit.getStart(), limit.getLimit());
        }

        // 如果符合条件则包装select的子查询查询出符合的去重的主键列表
        if (isChildSelect) {
            DefaultSQLSelectBuilder selectWrap = new DefaultSQLSelectBuilder();
            selectWrap.select();
            this.buildSelectField(selectWrap, alias, fieldAlias);
            selectWrap.from().table(select, query.getQueryTableAs());
            this.buildJoinQuery(query, selectWrap, alias, joins, false);
            this.buildOrderBy(query, selectWrap, alias, orders, joins, queryTable,
                    joins != null && joins.size() > 0);

            select = selectWrap;
        }

        if (query.isForUpdate()) {
            select.forUpdate();
        }

        SQLBuilderCombine combine = dialect.select(select.compile());
        Object result = this.runner.doHandler(new JDBCTraversing(TypeForRunner.SELECT,
                combine.getSql(), combine.getPlaceholders()));

        return this.buildMergeObjects(fieldAlias, query, convert, (List<ModelObject>) result);
    }

    public long count(DefaultQuery query) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        Wraps<Filter> logicWraps = query.getLogicWraps();
        Set<Join> joins = query.getJoins();

        Class<?> tableClass = query.getTableClass();
        boolean isMaster = query.isMaster();
        String slaveName = query.getSlaveName();

        sessionContext.setMaster(isMaster);
        sessionContext.setSlaveName(slaveName);

        Map<Object, String> alias = new HashMap<>();
        alias.put(query, query.getQueryTableAs());
        AtomicInteger i = new AtomicInteger(1);
        boolean hasJoins = false;
        if (joins != null && joins.size() > 0) {
            hasJoins = true;
            alias.put(query, query.getQueryTableAs());
            for (Join join : joins) {
                String joinAliasName = getJoinAlias(alias, join, i);
                alias.put(join, joinAliasName);
            }
        }

        DefaultSQLSelectBuilder select = new DefaultSQLSelectBuilder();
        select.select();
        MappingTable mappingTable = mappingGlobalWrapper.getMappingTable(tableClass);
        List<MappingField> pks = mappingTable.getMappingPrimaryKeyFields();
        Serializable[] params = new Serializable[1];
        if (pks != null && pks.size() == 1) {
            if (hasJoins) {
                params[0] = new FieldItem(query.getQueryTableAs(), pks.get(0).getMappingColumnName());
            } else {
                params[0] = new FieldItem(pks.get(0).getMappingColumnName());
            }
        } else {
            params[0] = 1;
        }
        select.count(params).as("count");

        if (hasJoins) {
            select.from().table(mappingTable.getMappingTableName(), query.getQueryTableAs());
        } else {
            select.from().table(mappingTable.getMappingTableName());
        }
        this.buildJoinQuery(query, select, alias, joins, true);
        if (logicWraps != null) select.where();
        this.buildWraps(select, mappingTable, logicWraps, hasJoins, query);

        SQLBuilderCombine combine = dialect.select(select.compile());
        Object result = this.runner.doHandler(new JDBCTraversing(TypeForRunner.SELECT,
                combine.getSql(), combine.getPlaceholders()));
        List<ModelObject> objects = (List<ModelObject>) result;
        if (objects != null && objects.size() > 0) {
            return objects.get(0).getIntValue("count");
        } else {
            return 0;
        }
    }

    public List<ModelObject> function(DefaultFunction f) throws SQLException {
        PlatformDialect dialect = this.getDialect();
        Set<FunctionField> funs = f.getFuns();
        Class tableClass = f.getTableClass();
        Wraps<Filter> logicWraps = f.getLogicWraps();
        Set groups = f.getGroupBy();
        Set<OrderBy> orders = f.getOrderBy();
        Set<HavingField> havingFields = f.getHavingFields();

        boolean isMaster = f.isMaster();
        String slaveName = f.getSlaveName();
        sessionContext.setMaster(isMaster);
        sessionContext.setSlaveName(slaveName);

        MappingTable mappingTable = mappingGlobalWrapper.getMappingTable(tableClass);

        DefaultSQLSelectBuilder select = new DefaultSQLSelectBuilder();
        select.select();
        for (FunctionField fun : funs) {
            Object field = fun.getField();
            BasicFunction function = fun.getFunction();
            String alias = fun.getAlias();
            boolean distinct = fun.isDistinct();
            // 处理精度
            int scale = fun.getScale();
            String avgCountName = fun.getAvgCountName();

            MappingField mappingField = mappingTable.getMappingFieldByJavaName(String.valueOf(field));
            String columnName = mappingField.getMappingColumnName();

            if (function == BasicFunction.AVG) {
                if (distinct) {
                    select.avg("DISTINCT", columnName);
                } else {
                    select.avg(columnName);
                }
            }
            if (function == BasicFunction.COUNT) {
                if (distinct) {
                    select.count("DISTINCT", columnName);
                } else {
                    select.count(columnName);
                }
            }
            if (function == BasicFunction.MAX) {
                if (distinct) {
                    select.max("DISTINCT", columnName);
                } else {
                    select.max(columnName);
                }
            }
            if (function == BasicFunction.MIN) {
                if (distinct) {
                    select.min("DISTINCT", columnName);
                } else {
                    select.min(columnName);
                }
            }
            if (function == BasicFunction.SUM) {
                if (distinct) {
                    select.sum("DISTINCT", columnName);
                } else {
                    select.sum(columnName);
                }
            }
            if (StringTools.isNotEmpty(alias)) {
                select.as(alias);
            }
        }
        select.from().table(mappingTable.getMappingTableName(), "T");
        if (logicWraps != null) select.where();
        this.buildWraps(select, mappingTable, logicWraps, false, null);

        // 如果having要求必须传入group by那么就传入主键
        if (havingFields != null && havingFields.size() > 0
                && (groups == null || groups.size() == 0)
                && dialect.isSelectHavingMustGroupBy()) {
            groups = new LinkedHashSet();
            List<MappingField> pks = mappingTable.getMappingPrimaryKeyFields();
            if (pks != null) {
                for (MappingField field : pks) {
                    groups.add(field.getMappingColumnName());
                }
            }
        }

        if (groups != null && groups.size() > 0) {
            select.groupBy();
            for (Object field : groups) {
                MappingField mappingField = mappingTable.getMappingFieldByJavaName(String.valueOf(field));
                String columnName = mappingField.getMappingColumnName();
                select.column(columnName);
            }
        }

        if (havingFields != null && havingFields.size() > 0) {
            select.having();
            for (HavingField field : havingFields) {
                BasicFunction function = field.getFunction();
                boolean distinct = field.isDistinct();
                DefaultFilter filter = (DefaultFilter) field.getFilter();
                if (filter == null) {
                    throw new IllegalArgumentException(I18n.print("fun_miss_filter"));
                }
                Object key = filter.getKey();
                String symbol = filter.getSymbol();

                MappingField mappingField = mappingTable.getMappingFieldByJavaName(String.valueOf(key));
                String columnName = mappingField.getMappingColumnName();

                if (!symbol.equalsIgnoreCase("isNull")
                        && !symbol.equalsIgnoreCase("isNotNull")) {
                    if (function == BasicFunction.AVG) {
                        if (distinct) {
                            select.avg("DISTINCT", columnName);
                        } else {
                            select.avg(columnName);
                        }
                    }
                    if (function == BasicFunction.COUNT) {
                        if (distinct) {
                            select.count("DISTINCT", columnName);
                        } else {
                            select.count(columnName);
                        }
                    }
                    if (function == BasicFunction.MAX) {
                        if (distinct) {
                            select.max("DISTINCT", columnName);
                        } else {
                            select.max(columnName);
                        }
                    }
                    if (function == BasicFunction.MIN) {
                        if (distinct) {
                            select.min("DISTINCT", columnName);
                        } else {
                            select.min(columnName);
                        }
                    }
                    if (function == BasicFunction.SUM) {
                        if (distinct) {
                            select.sum("DISTINCT", columnName);
                        } else {
                            select.sum(columnName);
                        }
                    }
                }

                this.setSymbol(select, filter, null, mappingTable, 1, null);
            }
        }

        this.buildOrderBy(null, select, null, orders, null, mappingTable, false);

        SQLBuilderCombine combine = dialect.select(select.compile());
        Object result = this.runner.doHandler(new JDBCTraversing(TypeForRunner.SELECT,
                combine.getSql(), combine.getPlaceholders()));

        return (List<ModelObject>) result;
    }

    private Set<MappingField> getSelectFields(Map<Class, List<String>> includes,
                                              Map<Class, List<String>> excludes,
                                              MappingTable mappingTable) {
        Set<MappingField> mappingFields = mappingTable.getMappingFields();
        Set<MappingField> nset = null;
        List<String> ild = null;
        if (includes != null) ild = includes.get(mappingTable.getMappingClass());
        List<String> eld = null;
        if (excludes != null) eld = excludes.get(mappingTable.getMappingClass());
        if (ild != null || eld != null) {
            for (MappingField field : mappingFields) {
                if (eld != null && eld.indexOf(field.getMappingFieldName()) >= 0) continue;
                if (ild != null && ild.indexOf(field.getMappingFieldName()) == -1) continue;
                if (nset == null) nset = new LinkedHashSet<>();
                nset.add(field);
            }
        }
        if (nset == null) return mappingFields;
        return nset;
    }

    private void buildSelectField(DefaultSQLSelectBuilder select,
                                  Map<Object, String> alias,
                                  Map<Object, List<SelectFieldAliasReference>> fieldAlias) {
        if (fieldAlias != null && fieldAlias.size() > 0) {
            Iterator<Map.Entry<Object, List<SelectFieldAliasReference>>> iterator = fieldAlias.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, List<SelectFieldAliasReference>> entry = iterator.next();
                Object key = entry.getKey();
                String tableAliasName = alias.get(key);
                List<SelectFieldAliasReference> value = entry.getValue();
                for (SelectFieldAliasReference reference : value) {
                    select.field(tableAliasName, reference.getFieldName(), reference.getFieldAliasName());
                }
            }
        }
    }

    private void buildJoinQuery(DefaultQuery query,
                                DefaultSQLSelectBuilder select,
                                Map<Object, String> alias,
                                Set<Join> joins,
                                boolean onlyInnerJoin) {
        if (joins != null && joins.size() > 0) {

            for (Join j : joins) {
                DefaultJoin join = (DefaultJoin) j;
                if (onlyInnerJoin && join.getJoinType() != 1) {
                    continue;
                }
                List<JoinOnFilter> ons = join.getOns();
                if (ons != null && ons.size() > 0) {
                    Class<?> table = join.getTable();
                    Class<?> mainTable = join.getMainTable();
                    String aliasName = alias.get(j);
                    Join parent = join.getParentJoin();

                    MappingTable mainMappingTable = this.mappingGlobalWrapper.getMappingTable(mainTable);
                    MappingTable mappingTable = this.mappingGlobalWrapper.getMappingTable(table);

                    if (join.getJoinType() == 0) { // left join
                        select.left().join();
                    }
                    if (join.getJoinType() == 1) { // inner join
                        select.inner().join();
                    }

                    select.table(mappingTable.getMappingTableName(), aliasName);
                    select.on();
                    Iterator<JoinOnFilter> iterator = ons.iterator();
                    while (iterator.hasNext()) {
                        String parentAliasName = null;
                        if (parent != null) {
                            parentAliasName = alias.get(parent);
                        } else {
                            parentAliasName = query.getQueryTableAs();
                        }
                        JoinOnFilter filter = iterator.next();
                        if (filter.isOn()) {
                            OnField field = filter.getOnField();
                            AliasTableField aliasTableField = this.getTableFieldByAsField(alias, mainMappingTable, field.getKey());
                            MappingField mainMappingField = aliasTableField.mappingField;
                            if (StringTools.isNotEmpty(aliasTableField.aliasName)) {
                                parentAliasName = aliasTableField.aliasName;
                            }
                            MappingField mappingField = mappingTable.getMappingFieldByJavaName(String.valueOf(field.getValue()));
                            if (mainMappingField == null) {
                                throw new IllegalArgumentException(I18n
                                        .print("miss_executor_mapping_field", String.valueOf(field.getKey()),
                                                mainMappingTable.getMappingClass().getSimpleName()));
                            }
                            if (mappingField == null) {
                                throw new IllegalArgumentException(I18n
                                        .print("miss_executor_mapping_field", String.valueOf(field.getValue()),
                                                mappingTable.getMappingClass().getSimpleName()));
                            }
                            String columnName = mainMappingField.getMappingColumnName();
                            String value = mappingField.getMappingColumnName();
                            String symbol = field.getSymbol();

                            // 最好校验每一个参数值
                            if (symbol.equalsIgnoreCase("like")) {
                                select.column(parentAliasName, columnName).like().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("in")) {
                                select.column(parentAliasName, columnName).in().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("notIn")) {
                                select.column(parentAliasName, columnName).nin().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("=")) {
                                select.column(parentAliasName, columnName).eq().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("!=")) {
                                select.column(parentAliasName, columnName).ne().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase(">")) {
                                select.column(parentAliasName, columnName).gt().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase(">=")) {
                                select.column(parentAliasName, columnName).gte().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("<")) {
                                select.column(parentAliasName, columnName).lt().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("<=")) {
                                select.column(parentAliasName, columnName).lte().column(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("isNull")) {
                                select.isNull(aliasName, value);
                            } else if (symbol.equalsIgnoreCase("notNull")) {
                                select.isNotNull(aliasName, value);
                            }
                        } else {
                            DefaultFilter f = (DefaultFilter) filter.getFilter();
                            this.setSymbol(select, f, aliasName, mappingTable, 0, null);
                        }
                        if (iterator.hasNext()) {
                            select.and();
                        }
                    }
                }
            }
        }
    }

    private void buildOrderBy(DefaultQuery query,
                              DefaultSQLSelectBuilder select,
                              Map<Object, String> alias,
                              Set<OrderBy> orders,
                              Set<Join> joins,
                              MappingTable mappingTable,
                              boolean isInnerSelect) {
        if (orders != null) {
            for (OrderBy order : orders) {
                boolean isAsc = order.isAsc();
                Object field = order.getField();
                MappingField mappingField = mappingTable.getMappingFieldByJavaName(String.valueOf(field));
                if (mappingField == null) {
                    throw new IllegalArgumentException(
                            I18n.print("miss_executor_mapping_field",
                                    String.valueOf(field), mappingTable.getMappingTableName())
                    );
                }
                if (isInnerSelect) {
                    select.orderBy().column(query != null ? query.getQueryTableAs() : "T", mappingField.getMappingColumnName());
                } else {
                    select.orderBy().column(mappingField.getMappingColumnName());
                }
                if (isAsc) select.asc();
                else select.desc();
            }
            if (joins != null && joins.size() > 0) {
                for (Join join : joins) {
                    DefaultJoin j = (DefaultJoin) join;
                    Set<OrderBy> sorts = j.getOrderBy();
                    if (sorts != null && sorts.size() > 0) {
                        String tableAlias = alias.get(join);
                        MappingTable queryTable = this.mappingGlobalWrapper.getMappingTable(j.getTableClass());
                        for (OrderBy order : sorts) {
                            boolean isAsc = order.isAsc();
                            Object field = order.getField();
                            MappingField mappingField = queryTable.getMappingFieldByJavaName(String.valueOf(field));
                            select.orderBy().column(tableAlias, mappingField.getMappingColumnName());
                            if (isAsc) select.asc();
                            else select.desc();
                        }
                    }
                }
            }
        }
    }

    private List<ModelObject> buildMergeObjects(Map<Object, List<SelectFieldAliasReference>> references,
                                                DefaultQuery query,
                                                ModelObjectConvertKey convert,
                                                List<ModelObject> os) {
        List<SelectFieldAliasReference> selectFields = null;
        if (references != null) {
            selectFields = new ArrayList<>();
            Iterator<Map.Entry<Object, List<SelectFieldAliasReference>>> iterator = references.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, List<SelectFieldAliasReference>> entry = iterator.next();
                List<SelectFieldAliasReference> ref = entry.getValue();
                selectFields.addAll(ref);
            }
        }

        List<MergeTree> mergeTrees = new ArrayList();
        MergeTree top = new MergeTree();
        top.setMainTable(query.getTableClass());
        top.setSelfTable(query.getTableClass());
        if (references != null) {
            List<SelectFieldAliasReference> mainFields = references.get(query);
            if (mainFields != null) {
                top.setMapperSelectFields(mainFields);
            }
        }
        mergeTrees.add(top);

        Set<Join> joins = query.getJoins();

        if (joins != null) {
            for (Join join : joins) {
                MergeTree jm = new MergeTree();
                jm.setJoin(join);
                DefaultJoin j = (DefaultJoin) join;
                if (!j.isMulti()) {
                    jm.setMulti(false);
                }
                Class<?> c1 = j.getMainTable();
                Class<?> c2 = j.getTable();
                jm.setExternalConnectionName(j.getAliasName());

                jm.setMainTable(c1);
                jm.setSelfTable(c2);
                //  jm.setTableAliasName(j.getTableClassAliasName());

                if (references != null) {
                    List<SelectFieldAliasReference> fields = references.get(join);
                    jm.setMapperSelectFields(fields);
                }

                if (((DefaultJoin) join).getParentJoin() == null) {
                    top.addChildren(jm);
                    jm.setParent(top);
                } else {
                    for (MergeTree m : mergeTrees) {
                        if (m.getJoin() == ((DefaultJoin) join).getParentJoin()) {
                            m.addChildren(jm);
                            jm.setParent(m);
                        }
                    }
                }

                mergeTrees.add(jm);
            }
        }

        ObjectMerge modelMerge = new DefaultObjectMerge();
        modelMerge.setMergeTree(top);
        modelMerge.setMapperSelectFields(selectFields);
        modelMerge.setMappingNamedConvert(convert);
        modelMerge.setMappingGlobalWrapper(mappingGlobalWrapper);
        return modelMerge.getMergeAfterObjects(os, query.getTableClass());
    }

    private void buildWraps(CommonSymbolBuilder builder,
                            MappingTable table,
                            Wraps<Filter> wraps,
                            boolean hasJoins,
                            DefaultQuery query) {
        if (wraps != null) {
            Iterator<WrapsObject<Filter>> wrapsIterator = wraps.iterator();
            while (wrapsIterator.hasNext()) {
                WrapsObject<Filter> filter = wrapsIterator.next();
                DefaultFilter where = (DefaultFilter) filter.getWhere();
                Wraps link = filter.getLink();
                CriteriaLogic logic = filter.getLogic();

                if (where != null) {
                    this.setSymbol(builder, where, hasJoins ? query.getQueryTableAs() : null, table, 0, query);
                } else if (link != null) {
                    SimpleCommonWhereBuilder builderWraps = new SimpleCommonWhereBuilder();
                    this.buildWraps(builderWraps, table, link, hasJoins, query);
                    if (builder instanceof SimpleCommonWhereBuilder) {
                        ((SimpleCommonWhereBuilder) builder).wrapper(builderWraps);
                    } else {
                        ((DefaultSQLSelectBuilder) builder).wrapper(builderWraps);
                    }
                }

                if (wrapsIterator.hasNext()) {
                    if (logic.equals(CriteriaLogic.AND)) {
                        ((AndBuilder) builder).and();
                    }
                    if (logic.equals(CriteriaLogic.OR)) {
                        ((OrBuilder) builder).or();
                    }
                }
            }
        }
    }

    private boolean setSymbol(CommonSymbolBuilder builder,
                              DefaultFilter filter,
                              String aliasName,
                              MappingTable table,
                              int type,
                              DefaultQuery query) {
        Object key = filter.getKey();
        String as = filter.getAs();
        if (key instanceof AsField) {
            as = ((AsField) key).getAlias();
            if (StringTools.isEmpty(as)) {
                throw new IllegalArgumentException("miss table alias name in AsField");
            }
            key = ((AsField) key).getField();
            if (key == null) {
                throw new IllegalArgumentException("miss query field in AsField");
            }
        }

        MappingField field = null;
        String columnName = null;
        if (StringTools.isEmpty(as)) {
            field = table.getMappingFieldByJavaName(String.valueOf(key));
            if (field == null) {
                throw new IllegalArgumentException(I18n.print("miss_symbol_field",
                        table.getMappingTableName(), "" + key));
            }
            columnName = field.getMappingColumnName();
        } else {
            if (query != null) {
                Set<Join> joins = query.getJoins();
                MappingTable asTable = null;
                if (joins != null) {
                    for (Join j : joins) {
                        DefaultJoin dj = (DefaultJoin) j;
                        if (dj.getAs().equals(as)) {
                            asTable = mappingGlobalWrapper.getMappingTable(dj.getTableClass());
                            field = asTable.getMappingFieldByJavaName(String.valueOf(key));
                            break;
                        }
                    }
                }
                if (asTable == null) {
                    throw new IllegalArgumentException(I18n.print("miss_symbol_as_table",
                            as, "" + key));
                }
                if (field == null) {
                    throw new IllegalArgumentException(I18n.print("miss_symbol_field",
                            asTable.getMappingTableName(), "" + key));
                }
                columnName = field.getMappingColumnName();
            } else {
                columnName = String.valueOf(filter.getKey());
            }
        }

        Object value = filter.getValue();
        Object startValue = filter.getStartValue();
        Object endValue = filter.getEndValue();
        String symbol = filter.getSymbol();

        if (StringTools.isNotEmpty(as)) {
            aliasName = as;
        }

        if (!symbol.equalsIgnoreCase("isNull")
                && !symbol.equalsIgnoreCase("notNull")) {
            if (type != 1) {
                if (StringTools.isNotEmpty(aliasName)) builder.column(aliasName, columnName);
                else builder.column(columnName);
            }
        }

        if (symbol.equalsIgnoreCase("=")) {
            builder.eq();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("in")) {
            builder.in();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("notIn")) {
            builder.in();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("like")) {
            builder.like();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("!=")) {
            builder.ne();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase(">")) {
            builder.gt();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase(">=")) {
            builder.gte();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("<")) {
            builder.lt();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("<=")) {
            builder.lte();
            builder.value(value);
        }
        if (symbol.equalsIgnoreCase("between")) {
            builder.between().section((Serializable) startValue, (Serializable) endValue);
            return false;
        }
        if (symbol.equalsIgnoreCase("isNull") && builder != null) {
            if (StringTools.isNotEmpty(aliasName)) {
                builder.isNull(aliasName, columnName);
            } else {
                builder.isNull(columnName);
            }
            return false;
        }
        if (symbol.equalsIgnoreCase("notNull") && builder != null) {
            if (StringTools.isNotEmpty(aliasName)) {
                builder.isNotNull(aliasName, columnName);
            } else {
                builder.isNotNull(columnName);
            }
            return false;
        }
        return true;
    }

    public Object dialect(StampAction stampAction) throws SQLException {
        PlatformDialect dialect = this.getDialect();
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
            return new DefaultDBRunner(sessionContext)
                    .doHandler(new JDBCTraversing(combine.getSql(), combine.getPlaceholders()));
        }
        return null;
    }

    public Object original(JDBCTraversing traversing) throws SQLException {
        return this.runner.doHandler(traversing);
    }

    private String getJoinAlias(Map<Object, String> alias, Join join, AtomicInteger integer) {
        String joinAliasName = ((DefaultJoin) join).getAs();
        if (StringTools.isEmpty(joinAliasName)) {
            joinAliasName = "T" + integer.incrementAndGet();
            while (isRepeatAlias(alias, joinAliasName)) {
                joinAliasName = "T" + integer.incrementAndGet();
            }
        } else if (isRepeatAlias(alias, joinAliasName)) {
            throw new IllegalArgumentException(I18n.print("repeat_alias_name", joinAliasName));
        }
        return joinAliasName;
    }

    private boolean isRepeatAlias(Map<Object, String> alias, String joinAliasName) {
        boolean repeat = false;
        Set<Map.Entry<Object, String>> set = alias.entrySet();
        for (Map.Entry<Object, String> entry : set) {
            if (joinAliasName.equals(entry.getValue())) {
                repeat = true;
                break;
            }
        }
        return repeat;
    }

    /**
     * 为了支持跨级判断
     *
     * @param alias
     * @param mainMappingTable
     * @param fieldKey
     * @return
     */
    private AliasTableField getTableFieldByAsField(Map<Object, String> alias, MappingTable mainMappingTable, Object fieldKey) {
        String aliasName = null;
        if (fieldKey instanceof AsField) {
            aliasName = ((AsField) fieldKey).getAlias();
            if (StringTools.isNotEmpty(aliasName)) {
                Class tableClass = null;
                for (Map.Entry<Object, String> entry : alias.entrySet()) {
                    if (aliasName.equalsIgnoreCase(entry.getValue())) {
                        Object key = entry.getKey();
                        if (key instanceof Query) {
                            tableClass = ((Query<?>) key).getTableClass();
                        } else if (key instanceof Join) {
                            tableClass = ((Join) key).getTableClass();
                        }
                    }
                }
                if (tableClass == null) {
                    throw new IllegalArgumentException("can't find mapping table alias by " + aliasName);
                }
                mainMappingTable = this.mappingGlobalWrapper.getMappingTable(tableClass);
            } else {
                throw new IllegalArgumentException("miss table alias name in AsField");
            }
            fieldKey = ((AsField) fieldKey).getField();
            if (fieldKey == null) {
                throw new IllegalArgumentException("miss query field in AsField");
            }
        }
        MappingField mappingField = mainMappingTable.getMappingFieldByJavaName(String.valueOf(fieldKey));
        return new AliasTableField(mappingField, aliasName);
    }

    class AliasTableField {
        MappingField mappingField;
        String aliasName;

        public AliasTableField(MappingField mappingField, String aliasName) {
            this.mappingField = mappingField;
            this.aliasName = aliasName;
        }
    }
}
