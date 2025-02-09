package org.mimosaframework.orm.mapping;


import org.mimosaframework.core.utils.StringTools;
import org.mimosaframework.orm.IDStrategy;
import org.mimosaframework.orm.annotation.Column;
import org.mimosaframework.orm.annotation.Index;
import org.mimosaframework.orm.annotation.IndexItem;
import org.mimosaframework.orm.annotation.Table;
import org.mimosaframework.orm.convert.ConvertType;
import org.mimosaframework.orm.convert.NamingConvert;
import org.mimosaframework.orm.i18n.I18n;
import org.mimosaframework.orm.strategy.AutoIncrementStrategy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultDisassembleMappingClass implements DisassembleMappingClass {
    private Class mappingClass;
    private NamingConvert convert;
    private String prefix;

    public DefaultDisassembleMappingClass(Class mappingClass, NamingConvert convert, String prefix) {
        this.mappingClass = mappingClass;
        this.convert = convert;
        this.prefix = prefix;
    }

    @Override
    public MappingTable getMappingTable() {
        Annotation annotation = mappingClass.getAnnotation(Table.class);
        Annotation indexAnn = mappingClass.getAnnotation(Index.class);
        Table table = null;
        Index index = null;
        if (annotation != null) table = (Table) annotation;
        if (indexAnn != null) index = (Index) indexAnn;
        if (table != null) {
            SpecificMappingTable mappingTable = new SpecificMappingTable();

            String tableName = table.value();
            if (tableName.equals("") && convert != null) {
                tableName = convert.convert(mappingClass.getSimpleName(), ConvertType.TABLE_NAME);
            }

            if (StringTools.isNotEmpty(prefix)) {
                tableName = convert.prefix(tableName, prefix);
            }

            mappingTable.setMappingClass(mappingClass);
            mappingTable.setMappingClassName(mappingClass.getSimpleName());
            mappingTable.setMappingTableName(tableName);
            if (StringTools.isNumber(table.engineName())) {
                mappingTable.setEngineName(table.engineName());
            }
            if (StringTools.isNotEmpty(table.charset())) {
                mappingTable.setEncoding(table.charset());
            }
            if (StringTools.isNotEmpty(table.version())) {
                mappingTable.setVersion(table.version());
            }


            this.disassembleFields(mappingTable, table);
            this.disassembleIndexes(mappingTable, index);

            Set<MappingField> mappingFields = mappingTable.getMappingFields();

            if (mappingFields != null) {
                // 检查自增字段的个数,只允许有一个自增字段
                short c = 0;
                for (MappingField field : mappingFields) {
                    if (field.isMappingAutoIncrement()) {
                        c++;
                    }
                }
                if (c > 1) {
                    throw new IllegalArgumentException(I18n.print("incr_field_one", tableName));
                }
            }
            return mappingTable;
        }

        return null;
    }

    private void disassembleIndexes(SpecificMappingTable mappingTable, Index index) {
        if (mappingTable != null && index != null) {
            IndexItem[] indexItems = index.value();
            if (indexItems != null && indexItems.length > 0) {
                for (IndexItem item : indexItems) {
                    String indexName = item.indexName();
                    String[] columns = item.columns();
                    boolean unique = item.unique();
                    if (StringTools.isEmpty(indexName)) {
                        throw new IllegalArgumentException(I18n.print("miss_table_index_name",
                                mappingTable.getMappingClassName()));
                    }
                    if (columns == null) {
                        throw new IllegalArgumentException(I18n.print("miss_table_index_columns",
                                mappingTable.getMappingClassName()));
                    }
                    List<MappingField> fields = new ArrayList<>();
                    for (String columnName : columns) {
                        MappingField mappingField = mappingTable.getMappingFieldByJavaName(columnName);
                        if (mappingField == null) {
                            throw new IllegalArgumentException(I18n.print("miss_table_index_column",
                                    mappingTable.getMappingClassName(), columnName));
                        }
                        fields.add(mappingField);
                    }
                    MappingIndex mappingIndex = new SpecificMappingIndex(indexName,
                            fields, unique ? IndexType.U : IndexType.D);
                    mappingTable.addMappingIndex(mappingIndex);
                }
            }
        }
    }

    private void disassembleFields(SpecificMappingTable mappingTable, Table table) {
        String fieldName = null;
        Column column = null;
        Object fieldObject = null;
        MappingField lastField = null;
        Class<? extends IDStrategy> strategy = null;
        Class<Timestamp> timestamp = null;

        int countTimeForUpdate = 0;
        boolean ignoreSuperclass = table.ignoreSuperclass();

        if (mappingClass.isEnum()) {
            for (Object o : mappingClass.getEnumConstants()) {
                fieldName = ((Enum) o).name();
                try {
                    column = o.getClass().getField(fieldName).getAnnotation(Column.class);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }

                if (column != null) {
                    if (column.type().equals(Timestamp.class)) {
                        if (timestamp != null && timestamp.equals(Timestamp.class)) {
                            throw new IllegalArgumentException(I18n.print("timestamp_one"));
                        }
                        timestamp = Timestamp.class;
                    }

                    if (column.strategy().equals(AutoIncrementStrategy.class)) {
                        if (strategy != null && strategy.equals(AutoIncrementStrategy.class)) {
                            throw new IllegalArgumentException(I18n.print("incr_field_one",
                                    mappingTable.getMappingTableName()));
                        }
                        strategy = AutoIncrementStrategy.class;

                        if (!column.pk()) {
                            throw new IllegalArgumentException(I18n.print("auto_strategy_pk",
                                    mappingClass.getSimpleName() + "." + fieldName));
                        }
                    }

                    if (column.timeForUpdate()) countTimeForUpdate++;


                    fieldObject = o;
                    MappingField newField = this.disassembleFieldItem(mappingTable, fieldName, column, fieldObject, true);
                    if (lastField != null) {
                        newField.setPrevious(lastField);
                    }
                    lastField = newField;
                }
            }
        } else {
            List<Field> allFields = new ArrayList<>();
            List<String> excludeFields = new ArrayList<>();
            loadAllFields(mappingClass, allFields, excludeFields, ignoreSuperclass);
            Field[] fields = allFields.toArray(new Field[]{});
            for (Field field : fields) {
                fieldName = field.getName();
                column = field.getAnnotation(Column.class);

                if (column != null) {
                    if (column.strategy().equals(AutoIncrementStrategy.class)) {
                        if (strategy != null && strategy.equals(AutoIncrementStrategy.class)) {
                            throw new IllegalArgumentException(I18n.print("incr_field_one", mappingTable.getMappingTableName()));
                        }
                        strategy = AutoIncrementStrategy.class;
                    }

                    // todo:校验一下column配置的类型是否和bean类型一致

                    fieldObject = field;
                    MappingField newField = this.disassembleFieldItem(mappingTable, fieldName, column, fieldObject, false);
                    if (lastField != null) {
                        newField.setPrevious(lastField);
                    }
                    lastField = newField;

                    if (column.timeForUpdate()) countTimeForUpdate++;
                }
            }
        }

        List<MappingField> pkfields = mappingTable.getMappingPrimaryKeyFields();
        if (pkfields == null || pkfields.size() == 0) {
            throw new IllegalArgumentException(I18n.print("must_have_pk",
                    mappingTable.getMappingTableName()));
        }

        if (countTimeForUpdate > 1) {
            throw new IllegalArgumentException(I18n.print("just_max_one_tfu",
                    mappingTable.getMappingTableName()));
        }
    }

    private void loadAllFields(Class c, List<Field> fields, List<String> excludeFields, boolean ignoreSuperclass) {
        if (!c.equals(Object.class)) {
            Field[] fds = c.getDeclaredFields();
            for (Field field : fds) {
                // 如果实现类已经存在则表示覆盖父配置以实现类为准
                if (excludeFields != null && excludeFields.contains(field.getName())) {
                    continue;
                }
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    fields.add(field);
                }
                excludeFields.add(field.getName());
            }
            if (ignoreSuperclass == false) {
                this.loadAllFields(c.getSuperclass(), fields, excludeFields, ignoreSuperclass);
            }
        }
    }

    private MappingField disassembleFieldItem(SpecificMappingTable mappingTable,
                                              String fieldName,
                                              Column column,
                                              Object fieldObject,
                                              boolean isEnum) {
        SpecificMappingField mappingField = new SpecificMappingField(mappingTable);
        mappingField.setMappingField(fieldObject);
        mappingField.setMappingFieldAnnotation(column);
        mappingField.setMappingFieldName(fieldName);
        String columnName = column.name();
        if (StringTools.isEmpty(columnName) && convert != null) {
            columnName = convert.convert(fieldName, ConvertType.FIELD_NAME);
        }

        mappingField.setMappingColumnName(columnName);
        if (column.pk() && column.type() == String.class && column.length() == 255) {
            // 如果是主键且没有配置
            mappingField.setMappingFieldType(long.class);
        } else {
            if (isEnum == false) {
                Class type = ((Field) fieldObject).getType();
                if (column.type() != String.class && column.type() != type) {
                    // 如果注解里使用了自定义的类型则注解优先
                    type = column.type();
                }
                mappingField.setMappingFieldType(type);
            } else {
                mappingField.setMappingFieldType(column.type());
            }
        }

        mappingField.setMappingFieldLength(column.length());
        mappingField.setMappingFieldDecimalDigits(column.scale());
        mappingField.setMappingFieldNullable(column.nullable());
        mappingField.setMappingFieldPrimaryKey(column.pk());
        mappingField.setMappingFieldIndex(column.index());
        mappingField.setMappingFieldUnique(column.unique());
        if (StringTools.isNotEmpty(column.comment())) {
            mappingField.setMappingFieldComment(column.comment());
        }
        mappingField.setMappingFieldTimeForUpdate(column.timeForUpdate());
        if (StringTools.isNotEmpty(column.defaultValue())) {
            mappingField.setMappingFieldDefaultValue(column.defaultValue());
        }

        Class c = mappingField.getMappingFieldType();
        if (Boolean.class == c || boolean.class == c) {
            String df = mappingField.getMappingFieldDefaultValue();
            if ("false".equalsIgnoreCase(df)) {
                mappingField.setMappingFieldDefaultValue("0");
            }
            if ("true".equalsIgnoreCase(df)) {
                mappingField.setMappingFieldDefaultValue("1");
            }
        }

        if (column.strategy().equals(AutoIncrementStrategy.class)) {
            mappingField.setMappingFieldAutoIncrement(true);
            if (mappingField.getMappingFieldType() != short.class
                    && mappingField.getMappingFieldType() != int.class
                    && mappingField.getMappingFieldType() != long.class
                    && mappingField.getMappingFieldType() != Short.class
                    && mappingField.getMappingFieldType() != Integer.class
                    && mappingField.getMappingFieldType() != Long.class) {
                throw new IllegalArgumentException(I18n.print("auto_field_type_error"));
            }
        }
        mappingTable.addMappingField(mappingField);
        if (mappingField.getMappingFieldType().equals(BigDecimal.class)
                && mappingField.getMappingFieldLength() == 255) {
            throw new IllegalArgumentException(I18n.print("must_set_decimal"));
        }

        return mappingField;
    }
}
