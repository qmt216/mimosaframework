package org.mimosaframework.orm.transaction;

import org.mimosaframework.core.utils.i18n.Messages;
import org.mimosaframework.orm.MimosaDataSource;
import org.mimosaframework.orm.exception.TransactionException;
import org.mimosaframework.orm.i18n.LanguageMessageFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.UUID;

public class NestedTransactionPropagation implements TransactionPropagation {
    private MimosaDataSource dataSource;
    private TransactionManager previousTransaction;
    private Transaction transaction;
    private Connection connection;
    private boolean isNewCreate = false;
    private Savepoint savepoint;
    private TransactionIsolationType it;

    public NestedTransactionPropagation(TransactionManager previousTransaction, TransactionIsolationType it) {
        this.it = it;
        this.previousTransaction = previousTransaction;
    }

    @Override
    public void setDataSource(MimosaDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws TransactionException {
        if (this.previousTransaction != null) {
            this.connection = this.previousTransaction.getConnection(dataSource);
        }

        if (this.connection == null) {
            try {
                connection = dataSource.getConnection(true, null, false);
                connection.setAutoCommit(false);
                if (it != null) {
                    connection.setTransactionIsolation(it.getCode());
                }
            } catch (SQLException e) {
                throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                        NestedTransactionPropagation.class, "create_trans_fail"), e);
            }
            isNewCreate = true;
        } else {
            if (savepoint == null) {
                try {
                    this.savepoint = connection.setSavepoint(UUID.randomUUID().toString().replaceAll("-", ""));
                } catch (SQLException e) {
                    throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                            NestedTransactionPropagation.class, "create_trans_point_fail"), e);
                }
                isNewCreate = false;
            }
        }
        return connection;
    }

    @Override
    public void commit() throws TransactionException {
        if (isNewCreate) {
            try {
                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                        NestedTransactionPropagation.class, "submit_trans_fail"), e);
            }
        }
    }

    @Override
    public void rollback() throws TransactionException {
        if (isNewCreate) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                        NestedTransactionPropagation.class, "rollback_trans_fail"), e);
            }
        } else {
            try {
                if (connection != null && savepoint != null) {
                    connection.rollback(savepoint);
                }
            } catch (SQLException e) {
                throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                        NestedTransactionPropagation.class, "rollback_trans_point_fail"), e);
            }
        }
    }

    @Override
    public void close() throws TransactionException {
        if (isNewCreate) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new TransactionException(Messages.get(LanguageMessageFactory.PROJECT,
                        NestedTransactionPropagation.class, "close_db_fail"), e);
            }
        }
    }

    @Override
    public boolean isAutoCommit() {
        return true;
    }
}
