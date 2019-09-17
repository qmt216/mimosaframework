<img src="https://repository-images.githubusercontent.com/207211209/39fc7180-d94f-11e9-8866-d05f91f10f31" width="600" align="center"/>

## MimosaFramework简介
MimosaFramework是一组框架组合，主要功能是提供数据库读写的工具。

框架分为三部分，第一部分mimosa-core包含一些基本的工具类以及ModelObject的JSON操作库(使用FastJson为基础)，以及cglib和ognl表达式等开源库(方便独立使用)。
第二部分mimosa-orm是数据库读写操作的核心库(依赖mimosa-core)，用过API操作数据库，通过注解类自动创建数据库表，并且可以提供类似Mybatis的xml配置SQL的方式执行SQL语句。
第三部分mimosa-mvc是在spring-mvc基础上扩展的方便Ajax交互的工具(依赖mimosa-core)，可以简化后台Ajax交互复杂度。

总的来说MimosaFramework可以给我们提供一个方便快捷开发的半自动框架。MimosaFramework并不能帮我们做全部的事情，很多时候依旧需要开发者本身去处理一些事情。

框架的参考文档点击这里 [参考文档](https://mimosaframework.org) 访问查看MimosaFramework详细教程。

### 开始使用Mimosa框架

##### 第一步、创建一个maven项目，并且引用jar包

```xml
<dependencies>
    <!--必须引入的核心包-->
    <dependency>
        <groupId>org.mimosaframework.core</groupId>
        <artifactId>mimosa-core</artifactId>
        <version>3.3.7</version>
    </dependency>

    <!--必须引入的核心包-->
    <dependency>
        <groupId>org.mimosaframework.orm</groupId>
        <artifactId>mimosa-orm</artifactId>
        <version>3.3.7</version>
    </dependency>

    <!--额外依赖的日志包-->
    <dependency>
        <groupId>commons-logging</groupId>
        <artifactId>commons-logging</artifactId>
        <version>1.2</version>
    </dependency>

    <!--数据库连接池包-->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
        <version>1.1.10</version>
    </dependency>

    <!--Mysql驱动包-->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.11</version>
    </dependency>
</dependencies>
```

以上jar包除了mimosa-core、mimosa-orm必须引入外，其他的依照实际情况引入。

##### 第二步、创建映射表包名称 com.study.test.tables 并创建映射类

```java
package com.study.test.tables;

import org.mimosaframework.orm.annotation.Column;
import org.mimosaframework.orm.annotation.Table;
import org.mimosaframework.orm.strategy.AutoIncrementStrategy;

import java.util.Date;

@Table
public enum TableUser {
    @Column(pk = true, type = long.class, strategy = AutoIncrementStrategy.class)
    id,
    @Column(length = 64)
    userName,
    @Column(length = 64)
    password,
    @Column(length = 30)
    realName,
    @Column(type = int.class)
    age,
    @Column(type = int.class, defaultValue = "2")
    level,
    @Column(length = 20)
    address,
    @Column(type = Date.class)
    createdTime,
    @Column(timeForUpdate = true)
    modifiedDate
}
```

PS:映射类使用枚举类，类上必须使用注解 @Table 作为标识，每个枚举对象使用 @Column 修饰配置表字段信息。

##### 第三步、创建框架配置文件 mimosa.xml 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<mimosa name="mimosa_test" description="测试用的配置">
    <convert name="H2U"/>
    <mapping scan="com.study.test.tables"/>
    <format showSql="true"/>
    <datasource wrapper="default"/>

    <wrappers>
        <wrapper name="default" master="master"/>
    </wrappers>

    <dslist>
        <ds name="master">
            <property name="dataSourceClass">com.alibaba.druid.pool.DruidDataSource</property>
            <property name="driverClassName">com.mysql.jdbc.Driver</property>
            <property name="url">jdbc:mysql://localhost:3306/mimosa?useUnicode=true&amp;characterEncoding=utf-8&amp;useSSL=false&amp;serverTimezone=UTC&amp;nullNamePatternMatchesAll=true
            </property>
            <property name="username">root</property>
            <property name="password">12345</property>
            <!--初始化的连接数-->
            <property name="initialSize" value="5"/>
            <property name="maxActive" value="10000"/>
            <property name="maxWait" value="60000"/>
            <property name="validationQuery" value="select 1"/>
            <property name="testWhileIdle" value="true"/>
            <property name="timeBetweenEvictionRunsMillis" value="3600000"/>
            <property name="minEvictableIdleTimeMillis" value="18000000"/>
            <property name="testOnBorrow" value="true"/>
        </ds>
    </dslist>
</mimosa>
```
convert: 表字段映射驼峰转下户线配置

mapping: 扫描的包名称，将注解@Table的类作为映射类并创建表

format: 配置一些特殊信息，比如是否打印SQL到控制台

wrappers: 数据源束，一个数据源束分为一个主库和若干个从库组成，主库不能为空，从库可以没有

datasource: 当前使用的数据源束

dslist: 数据源列表，可以配置多个数据源供数据源束使用

##### 第四步、初始化并使用Mimosa框架

```java
package com.study.test;

import com.study.test.tables.TableUser;
import org.mimosaframework.core.json.ModelObject;
import org.mimosaframework.orm.*;
import org.mimosaframework.orm.exception.ContextException;

import java.util.Date;

public class Start {
    public static void main(String[] args) throws ContextException {
        XmlAppContext context = new XmlAppContext(SessionFactoryBuilder.class.getResourceAsStream("/mimosa.xml"));
        SessionFactory sessionFactory = context.getSessionFactoryBuilder().build();
        SessionTemplate template = new MimosaSessionTemplate();
        ((MimosaSessionTemplate) template).setSessionFactory(sessionFactory);

        ModelObject object = new ModelObject(TableUser.class);
        object.put(TableUser.id, 20);
        object.put(TableUser.userName, "yangankang_test_save_n_2");
        object.put(TableUser.password, "123456");
        object.put(TableUser.realName, "北京简子行科技有限公司");
        object.put(TableUser.address, "北京朝阳区");
        object.put(TableUser.age, 25);
        object.put(TableUser.level, 10);
        object.put(TableUser.createdTime, new Date());
        template.saveAndUpdate(object);
    }
}
```

结束: 接下来你可以看到数据库中会插入一条记录。以上是mimosa-orm的基本用法，详细用法请到[官方文档](https://mimosaframework.org)查看
