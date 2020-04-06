package org.mimosaframework.orm.sql.stamp;

import java.util.ArrayList;
import java.util.List;

public class StampDelete implements StampAction {
    public StampFrom from;
    public StampWhere where;

    @Override
    public List<STItem> getTables() {
        List<STItem> items = new ArrayList<>();
        if (from != null) {
            items.add(new STItem(from.table, from.aliasName));
        }
        return items;
    }
}
