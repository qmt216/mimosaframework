package org.mimosaframework.orm.spring;

import org.mimosaframework.orm.Session;
import org.mimosaframework.orm.SessionFactory;
import org.mimosaframework.orm.SessionHolder;
import org.mimosaframework.orm.exception.MimosaException;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

public class SpringSessionHolder implements SessionHolder {
    private SessionFactory factory;
    private Session session;

    @Override
    public Session getSession(SessionFactory factory) throws MimosaException {
        this.factory = factory;
        SessionHolderResource resource = (SessionHolderResource) TransactionSynchronizationManager.getResource(factory);
        if (resource != null) {
            return resource.session;
        }

        Session session = factory.openSession();
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && factory.getConfiguration().getTransactionFactory() instanceof SpringTransactionFactory) {
            resource = new SessionHolderResource(session);
            TransactionSynchronizationManager.bindResource(factory, resource);
            TransactionSynchronizationManager
                    .registerSynchronization(new SpringSynchronizationAdapter(resource, factory));
            resource.setSynchronizedWithTransaction(true);
            resource.requested();
        }
        this.session = session;

        return session;
    }

    @Override
    public boolean begin() {
        return false;
    }

    @Override
    public boolean isSessionTransactional(Session session) {
        SessionHolderResource holder = (SessionHolderResource) TransactionSynchronizationManager.getResource(factory);
        return holder != null && holder.getSession() == session;
    }

    @Override
    public boolean end() {
        return false;
    }

    @Override
    public boolean close() {
        SessionHolderResource resource = (SessionHolderResource) TransactionSynchronizationManager.getResource(factory);
        if (resource != null) {
            // 释放spring的事务资源
            resource.released();
        } else if (this.session != null && !this.isSessionTransactional(this.session)) {
            // 如果没有加入到spring的事务中则自己主动关闭session
            try {
                this.session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    class SessionHolderResource extends ResourceHolderSupport {
        private final Session session;

        public SessionHolderResource(Session session) {
            this.session = session;
        }

        public Session getSession() {
            return session;
        }
    }
}
