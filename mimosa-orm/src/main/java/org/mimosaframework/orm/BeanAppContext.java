package org.mimosaframework.orm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mimosaframework.core.json.ModelObject;
import org.mimosaframework.orm.auxiliary.FactoryBuilder;
import org.mimosaframework.orm.builder.*;
import org.mimosaframework.orm.exception.ContextException;
import org.mimosaframework.orm.i18n.I18n;
import org.mimosaframework.orm.mapping.CompareMappingFactory;
import org.mimosaframework.orm.mapping.StartCompareMapping;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class BeanAppContext implements Context {
    private static final Log logger = LogFactory.getLog("init");
    protected final NormalContextContainer contextValues;
    private SessionFactoryBuilder sessionFactoryBuilder = null;
    private ConfigBuilder configBuilder = null;

    static {
        I18n.register();
    }

    public BeanAppContext() {
        this.contextValues = new NormalContextContainer();
    }

    public BeanAppContext(NormalContextContainer contextValues) {
        this.contextValues = contextValues;
    }

    public BeanAppContext(ConfigBuilder configBuilder) throws ContextException {
        this();
        setBeanAppContext(configBuilder);
    }

    public void setBeanAppContext(ConfigBuilder configBuilder) throws ContextException {
        this.sessionFactoryBuilder = new MimosaSessionFactoryBuilder(contextValues);
        this.configBuilder = configBuilder;
        this.init();
    }

    protected void init() throws ContextException {
        contextValues.setContext(this);

        {
            CenterConfigSetting center = this.configBuilder.getCenterInfo();
            if (center != null && center.valid()) {
                // 从配置中心拉取配置
                this.configBuilder = new ConfiguredCenterBuilder();
            }
        }

        {
            ApplicationSetting applicationInfo = this.configBuilder.getApplication();
            contextValues.setApplicationName(applicationInfo.getApplicationName());
            contextValues.setApplicationDetail(applicationInfo.getApplicationDetail());

            List<? extends IDStrategy> strategies = this.configBuilder.getStrategies();
            contextValues.setIdStrategies(strategies);

            System.out.println("" +
                    "------------------------------------------------------------------------------------------------------------\n" +
                    "|     .---.                                                                                                |\n" +
                    "|     |   |                                                                                                |\n" +
                    "|     '---' .--.               _..._                .--.                      .--.    _..._                |\n" +
                    "|     .---. |__|             .'     '.              |__|                      |__|  .'     '.    .--./)    |\n" +
                    "|     |   | .--.            .   .-.   .             .--.                      .--. .   .-.   .  /.''\\\\     |\n" +
                    "|     |   | |  |     __     |  '   '  |             |  |    ____     _____    |  | |  '   '  | | |  | |    |\n" +
                    "|     |   | |  |  .:--.'.   |  |   |  | .--------.  |  |   `.   \\  .'    /    |  | |  |   |  |  \\`-' /     |\n" +
                    "|     |   | |  | / |   \\ |  |  |   |  | |____    |  |  |     `.  `'    .'     |  | |  |   |  |  /(\"'`      |\n" +
                    "|     |   | |  | `\" __ | |  |  |   |  |     /   /   |  |       '.    .'       |  | |  |   |  |  \\ '---.    |\n" +
                    "|     |   | |__|  .'.''| |  |  |   |  |   .'   /    |__|       .'     `.      |__| |  |   |  |   /'\"\"'.\\   |\n" +
                    "|  __.'   '      / /   | |_ |  |   |  |  /    /___           .'  .'`.   `.         |  |   |  |  ||     ||  |\n" +
                    "| |      '       \\ \\._,\\ '/ |  |   |  | |         |        .'   /    `.   `.       |  |   |  |  \\'. __//   |\n" +
                    "| |____.'         `--'  `\"  '--'   '--' |_________|       '----'       '----'      '--'   '--'   `'---'    |\n" +
                    "------------------------------------------------------------------------------------------------------------\n" +
                    "|                                      感谢您使用简子行科技有限公司产品                                          |\n" +
                    "|                                   本公司开源产品完全免费，但请遵守开源协议                                      |\n" +
                    "|                              如您需要定制请访问(http://www.jianzixing.com.cn)                                |\n" +
                    "------------------------------------------------------------------------------------------------------------\n"
            );
        }

        {
            BasicSetting basicInfo = this.configBuilder.getBasicInfo();
            if (basicInfo.getConvert() != null) {
                contextValues.setConvert(basicInfo.getConvert());
            }
            contextValues.setShowSQL(basicInfo.isShowSQL());
            contextValues.setMappingLevel(basicInfo.getMappingLevel());
            if (basicInfo.isIgnoreEmptySlave() != null) {
                contextValues.setIgnoreEmptySlave(basicInfo.isIgnoreEmptySlave());
            }

            contextValues.setInterceptSession(basicInfo.getInterceptSession());
        }

        {
            List<MimosaDataSource> dslist = null;
            try {
                dslist = this.configBuilder.getDataSources();
            } catch (SQLException e) {
                throw new ContextException(I18n.print("get_ds_list_fail"), e);
            }
            if (dslist != null) {
                for (MimosaDataSource ds : dslist) {
                    contextValues.addMimosaDataSource(ds);
                }
            }
        }

        {
            Set<Class> resolvers = this.configBuilder.getResolvers();
            contextValues.setDisassembleResolvers(resolvers);
        }

        {
            List<String> mappers = this.configBuilder.getMappers();
            contextValues.setMappers(mappers);
        }

        {
            List<FactoryBuilder> factoryBuilder = null;
            try {
                factoryBuilder = this.configBuilder.getAuxFactoryBuilder();
            } catch (Exception e) {
                throw new ContextException(I18n.print("init_tool_error"), e);
            }
            contextValues.setFactoryBuilderList(factoryBuilder);
        }

        this.checkDBMapping();
        ModelObject.addChecker(new ModelMeasureChecker(contextValues.getMappingTables()));
    }

    @Override
    public SessionFactoryBuilder getSessionFactoryBuilder() {
        return this.sessionFactoryBuilder;
    }

    protected void checkDBMapping() throws ContextException {
        try {
            StartCompareMapping compareMapping = CompareMappingFactory.getCompareMapping(
                    contextValues.getMappingLevel(),
                    contextValues.mappingGlobalWrapper,
                    contextValues.getDefaultDataSourceWrapper(false)
            );
            compareMapping.doMapping();
        } catch (SQLException e) {
            throw new ContextException(I18n.print("compare_db_error"), e);
        }
    }
}
