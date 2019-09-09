package org.mimosaframework.orm.strategy;

import org.mimosaframework.core.json.ModelObject;
import org.mimosaframework.orm.ContextValues;
import org.mimosaframework.orm.IDStrategy;
import org.mimosaframework.orm.Session;
import org.mimosaframework.orm.annotation.Column;
import org.mimosaframework.orm.exception.StrategyException;
import org.mimosaframework.orm.mapping.MappingField;
import org.mimosaframework.orm.mapping.MappingTable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StrategyFactory {
    private static final Map<Class, IDStrategy> strategys = new ConcurrentHashMap<>();

    public static Map<MappingField, IDStrategy> getStrategy(ContextValues values, MappingTable table) {
        Set<MappingField> fields = table.getMappingFields();
        Map<MappingField, IDStrategy> map = new HashMap<>();
        for (MappingField f : fields) {
            Column column = f.getMappingFieldAnnotation();
            if (column != null && strategys.get(column.strategy()) != null) {
                map.put(f, strategys.get(column.strategy()));
            }
        }
        return map;
    }

    /**
     * MappingTable 是Class的映射类，不包含数据库字段
     *
     * @param values
     * @param table
     * @param object
     */
    public static void applyStrategy(ContextValues values, MappingTable table, ModelObject object, Session session, boolean isUsedDistributedStrategy) throws StrategyException {
        Set<MappingField> fields = table.getMappingFields();
        for (MappingField f : fields) {
            Column column = f.getMappingFieldAnnotation();
            if (column != null) {
                // 如果表没有分表则使用数据库默认规则
                Class<? extends IDStrategy> c = column.strategy();
                // 如果表已经分表则使用默认的数据库ID自增策略
                if (c == AutoIncrementStrategy.class
                        && isUsedDistributedStrategy) {
                    c = DistributedAutoIncrementStrategy.class;
                }

                if (c != AutoIncrementStrategy.class && c != IDStrategy.class) {
                    if (strategys.get(c) == null) {
                        // 假如有已经实例好的ID生成策略对象，就用已经生成好的对象
                        List<? extends IDStrategy> list = values.getIdStrategies();
                        if (list != null && list.size() > 0) {
                            for (IDStrategy strategy : list) {
                                if (strategy.getClass().isAssignableFrom(c)) {
                                    strategys.put(c, strategy);
                                }
                            }
                        }

                        if (strategys.get(c) == null) {
                            try {
                                strategys.put(c, c.newInstance());
                            } catch (Exception e) {
                                throw new StrategyException("创建ID策略出错", e);
                            }
                        }
                    }
                    IDStrategy strategy = strategys.get(c);
                    StrategyWrapper wrapper = new StrategyWrapper(values);
                    wrapper.setTableName(table.getMappingTableName());
                    wrapper.setDbTableName(table.getMappingTableName());
                    wrapper.setField(f.getMappingFieldName());
                    wrapper.setDbField(f.getMappingColumnName());
                    wrapper.setTableClass(table.getMappingClass());
                    Serializable serializable = strategy.get(wrapper, session);
                    object.put(f.getMappingFieldName(), serializable);
                }
            }
        }
    }
}
