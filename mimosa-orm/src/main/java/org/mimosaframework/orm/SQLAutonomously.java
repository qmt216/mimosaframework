package org.mimosaframework.orm;

import org.mimosaframework.core.json.ModelObject;
import org.mimosaframework.core.utils.AssistUtils;
import org.mimosaframework.orm.i18n.I18n;
import org.mimosaframework.orm.sql.SQLActionFactory;
import org.mimosaframework.orm.sql.StructureBuilder;
import org.mimosaframework.orm.sql.UnifyBuilder;
import org.mimosaframework.orm.sql.delete.DeleteStartBuilder;
import org.mimosaframework.orm.sql.insert.InsertStartBuilder;
import org.mimosaframework.orm.sql.select.SelectStartBuilder;
import org.mimosaframework.orm.sql.update.UpdateStartBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SQLAutonomously implements Serializable {
    private List<LinkAutonomously> dataSourceLinks;
    private String sql;
    private UnifyBuilder builder;
    private boolean isMaster = true;
    private String slaveName;
    private ModelObject parameter;

    public SQLAutonomously() {
    }

    public SQLAutonomously(String sql, ModelObject parameter) {
        this.sql = sql;
        this.parameter = parameter;
    }

    public SQLAutonomously(String sql, ModelObject parameter, String slaveName) {
        this.sql = sql;
        this.slaveName = slaveName;
        this.parameter = parameter;
    }

    public SQLAutonomously(String sql) {
        this.sql = sql;
    }

    public SQLAutonomously(String sql, boolean isMaster) {
        this.sql = sql;
        this.isMaster = isMaster;
    }

    public SQLAutonomously(String sql, String slaveName) {
        this.sql = sql;
        this.isMaster = true;
        this.slaveName = slaveName;
    }

    public SQLAutonomously(UnifyBuilder builder) {
        this.builder = builder;
    }

    public SQLAutonomously(UnifyBuilder builder, boolean isMaster) {
        this.builder = builder;
        this.isMaster = isMaster;
    }

    public SQLAutonomously(UnifyBuilder builder, String slaveName) {
        this.builder = builder;
        this.isMaster = true;
        this.slaveName = slaveName;
    }

    public static SQLAutonomously newInstance() {
        return new SQLAutonomously();
    }

    public static SQLAutonomously newInstance(String sql) {
        return new SQLAutonomously(sql);
    }

    public static SQLAutonomously newInstance(String sql, ModelObject parameter) {
        return new SQLAutonomously(sql, parameter);
    }

    public static SQLAutonomously newInstance(String sql, boolean isMaster) {
        return new SQLAutonomously(sql, isMaster);
    }

    public static SQLAutonomously newInstance(String sql, String slaveName) {
        return new SQLAutonomously(sql, slaveName);
    }

    public static SQLAutonomously newInstance(UnifyBuilder builder) {
        return new SQLAutonomously(builder);
    }

    public static SQLAutonomously newInstance(UnifyBuilder builder, boolean isMaster) {
        return new SQLAutonomously(builder, isMaster);
    }

    public static SQLAutonomously newInstance(UnifyBuilder builder, String slaveName) {
        return new SQLAutonomously(builder, slaveName);
    }

    public static DeleteStartBuilder delete() {
        DeleteStartBuilder deleteStartBuilder = SQLActionFactory.delete();
        return deleteStartBuilder;
    }

    public static InsertStartBuilder insert() {
        InsertStartBuilder insertStartBuilder = SQLActionFactory.insert();
        return insertStartBuilder;
    }

    public static SelectStartBuilder select() {
        SelectStartBuilder selectStartBuilder = SQLActionFactory.select();
        return selectStartBuilder;
    }

    public static UpdateStartBuilder update() {
        UpdateStartBuilder updateStartBuilder = SQLActionFactory.update();
        return updateStartBuilder;
    }

    public static StructureBuilder structure() {
        StructureBuilder structureBuilder = new StructureBuilder();
        return structureBuilder;
    }

    public void add(String sql) {
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(new LinkAutonomously(sql));
    }

    public void add(String sql, boolean isMaster) {
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(new LinkAutonomously(sql, isMaster));
    }

    public void add(String dataSourceName, String sql) {
        AssistUtils.isNull(dataSourceName, I18n.print("not_empty"));
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(new LinkAutonomously(dataSourceName, sql));
    }

    public void add(String dataSourceName, String sql, boolean isMaster) {
        AssistUtils.isNull(dataSourceName, I18n.print("not_empty"));
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(new LinkAutonomously(dataSourceName, sql, isMaster));
    }

    public void add(String dataSourceName, String sql, String slaveName) {
        AssistUtils.isNull(dataSourceName, I18n.print("not_empty"));
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(new LinkAutonomously(dataSourceName, sql, slaveName));
    }

    public void add(LinkAutonomously autonomously) {
        if (dataSourceLinks == null) dataSourceLinks = new ArrayList<>();
        dataSourceLinks.add(autonomously);
    }

    public List<LinkAutonomously> getDataSourceLinks() {
        return dataSourceLinks;
    }

    public String getSql() {
        return sql;
    }

    public UnifyBuilder getBuilder() {
        return builder;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public boolean isMaster(String dsname) {
        if (dataSourceLinks != null) {
            for (LinkAutonomously autonomously : this.dataSourceLinks) {
                if (autonomously.getDataSourceName().equals(dsname)) {
                    return autonomously.isMaster();
                }
            }
        }
        return true;
    }

    public String getSlaveName() {
        return slaveName;
    }

    public String getSlaveName(String dsname) {
        if (dataSourceLinks != null) {
            for (LinkAutonomously autonomously : this.dataSourceLinks) {
                if (autonomously.getDataSourceName().equals(dsname)) {
                    return autonomously.getSlaveDataSourceName();
                }
            }
        }
        return null;
    }

    public ModelObject getParameter() {
        return parameter;
    }

    public void setParameter(ModelObject parameter) {
        this.parameter = parameter;
    }

    public static LinkAutonomously buildLinkAutonomously() {
        return new LinkAutonomously();
    }

    /**
     * 不同数据源执行不同的SQL语句
     */
    public static class LinkAutonomously {
        private String dataSourceName = MimosaDataSource.DEFAULT_DS_NAME;
        private boolean master = true;
        private String slaveDataSourceName;
        private String sql;
        private UnifyBuilder builder;

        public LinkAutonomously() {
        }

        public LinkAutonomously(String sql) {
            this.sql = sql;
        }

        public LinkAutonomously(String sql, boolean master) {
            this.master = master;
            this.sql = sql;
        }


        public LinkAutonomously(String dataSourceName, String sql) {
            this.dataSourceName = dataSourceName;
            this.sql = sql;
        }

        public LinkAutonomously(String dataSourceName, String sql, boolean master) {
            this.dataSourceName = dataSourceName;
            this.master = master;
            this.sql = sql;
        }

        public LinkAutonomously(String dataSourceName, String sql, String slaveDataSourceName) {
            this.dataSourceName = dataSourceName;
            this.slaveDataSourceName = slaveDataSourceName;
            this.sql = sql;
            this.master = false;
        }

        public LinkAutonomously(UnifyBuilder builder) {
            this.builder = builder;
        }

        public LinkAutonomously(UnifyBuilder builder, boolean master) {
            this.master = master;
            this.builder = builder;
        }


        public LinkAutonomously(String dataSourceName, UnifyBuilder builder) {
            this.dataSourceName = dataSourceName;
            this.builder = builder;
        }

        public LinkAutonomously(String dataSourceName, UnifyBuilder builder, boolean master) {
            this.dataSourceName = dataSourceName;
            this.master = master;
            this.builder = builder;
        }

        public LinkAutonomously(String dataSourceName, UnifyBuilder builder, String slaveDataSourceName) {
            this.dataSourceName = dataSourceName;
            this.slaveDataSourceName = slaveDataSourceName;
            this.builder = builder;
            this.master = false;
        }

        public String getDataSourceName() {
            return dataSourceName;
        }

        public void setDataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
        }

        public boolean isMaster() {
            return master;
        }

        public void setMaster(boolean master) {
            this.master = master;
        }

        public String getSlaveDataSourceName() {
            return slaveDataSourceName;
        }

        public void setSlaveDataSourceName(String slaveDataSourceName) {
            this.slaveDataSourceName = slaveDataSourceName;
        }

        public String getSql() {
            return sql;
        }

        public UnifyBuilder getBuilder() {
            return builder;
        }
    }
}
