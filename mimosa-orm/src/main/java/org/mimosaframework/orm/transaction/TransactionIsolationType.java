package org.mimosaframework.orm.transaction;

import java.sql.Connection;

/**
 * 值得一提的是：大多数数据库默认的事务隔离级别是Read committed，比如Sql Server , Oracle。Mysql的默认隔离级别是Repeatable read。
 */
public enum TransactionIsolationType {
    /**
     * 读未提交，顾名思义，就是一个事务可以读取另一个未提交事务的数据。
     * <p>
     * 事例：老板要给程序员发工资，程序员的工资是3.6万/月。但是发工资时老板不小心按错了数字，按成3.9万/月，
     * 该钱已经打到程序员的户口，但是事务还没有提交，就在这时，程序员去查看自己这个月的工资，发现比往常多了3千元，
     * 以为涨工资了非常高兴。但是老板及时发现了不对，马上回滚差点就提交了的事务，将数字改成3.6万再提交。
     * <p>
     * 分析：实际程序员这个月的工资还是3.6万，但是程序员看到的是3.9万。他看到的是老板还没提交事务时的数据。这就是脏读。
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

    /**
     * 读提交，顾名思义，就是一个事务要等另一个事务提交后才能读取数据。
     * <p>
     * 事例：程序员拿着信用卡去享受生活（卡里当然是只有3.6万），当他埋单时（程序员事务开启），
     * 收费系统事先检测到他的卡里有3.6万，就在这个时候！！程序员的妻子要把钱全部转出充当家用，并提交。
     * 当收费系统准备扣款时，再检测卡里的金额，发现已经没钱了（第二次检测金额当然要等待妻子转出金额事务提交完）。
     * 程序员就会很郁闷，明明卡里是有钱的…
     * <p>
     * 分析：这就是读提交，若有事务对数据进行更新（UPDATE）操作时，读操作事务要等待这个更新操作事务提交后才能读取数据，可以解决脏读问题。但在这个事例中，出现了一个事务范围内两个相同的查询却返回了不同数据，这就是不可重复读。
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    /**
     * 重复读，就是在开始读取数据（事务开启）时，不再允许修改操作
     * <p>
     * 事例：程序员拿着信用卡去享受生活（卡里当然是只有3.6万），当他埋单时（事务开启，不允许其他事务的UPDATE修改操作），
     * 收费系统事先检测到他的卡里有3.6万。这个时候他的妻子不能转出金额了。接下来收费系统就可以扣款了。
     * <p>
     * 分析：重复读可以解决不可重复读问题。写到这里，应该明白的一点就是，不可重复读对应的是修改，即UPDATE操作。
     * 但是可能还会有幻读问题。因为幻读问题对应的是插入INSERT操作，而不是UPDATE操作。
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    /**
     * Serializable 是最高的事务隔离级别，在该级别下，事务串行化顺序执行，可以避免脏读、不可重复读与幻读。
     * 但是这种事务隔离级别效率低下，比较耗数据库性能，一般不使用。
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);
    private int code;

    TransactionIsolationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
