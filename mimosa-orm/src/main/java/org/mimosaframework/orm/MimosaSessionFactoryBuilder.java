package org.mimosaframework.orm;

import org.mimosaframework.orm.exception.ContextException;

public class MimosaSessionFactoryBuilder implements SessionFactoryBuilder {
    private NormalContextContainer contextValues;

    public MimosaSessionFactoryBuilder(NormalContextContainer contextValues) {
        this.contextValues = contextValues;
    }

    @Override
    public SessionFactory build() throws ContextException {
        if (this.contextValues == null) {
            throw new ContextException("没有初始化上下文");
        }
        return new MimosaSessionFactory(this.contextValues);
    }
}
