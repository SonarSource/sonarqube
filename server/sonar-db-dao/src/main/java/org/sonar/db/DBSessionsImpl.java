/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.defaults.DefaultSqlSession;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;

public class DBSessionsImpl implements DBSessions {
  private static final Logger LOG = Loggers.get(DBSessionsImpl.class);

  private static final ThreadLocal<Boolean> CACHING_ENABLED = ThreadLocal.withInitial(() -> Boolean.FALSE);
  private final ThreadLocal<DelegatingDbSessionSupplier> regularDbSession = ThreadLocal.withInitial(this::buildRegularDbSessionSupplier);
  private final ThreadLocal<DelegatingDbSessionSupplier> batchDbSession = ThreadLocal.withInitial(this::buildBatchDbSessionSupplier);

  private final MyBatis myBatis;

  public DBSessionsImpl(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  private DelegatingDbSessionSupplier buildRegularDbSessionSupplier() {
    LOG.trace("{} called buildRegularDbSessionSupplier", currentThread());
    return new DelegatingDbSessionSupplier(() -> {
      DbSession res = myBatis.openSession(false);
      ensureAutoCommitFalse(res);
      LOG.trace("{} created regular DbSession {}", currentThread(), res);
      return res;
    });
  }

  private DelegatingDbSessionSupplier buildBatchDbSessionSupplier() {
    LOG.trace("{} called buildBatchDbSessionSupplier", currentThread());
    return new DelegatingDbSessionSupplier(() -> {
      DbSession res = myBatis.openSession(true);
      ensureAutoCommitFalse(res);
      LOG.trace("{} created batch DbSession {}", currentThread(), res);
      return res;
    });
  }

  private static void ensureAutoCommitFalse(DbSession dbSession) {
    try {
      SqlSession sqlSession = dbSession.getSqlSession();
      if (sqlSession instanceof DefaultSqlSession) {
        Field f = sqlSession.getClass().getDeclaredField("autoCommit");
        f.setAccessible(true);
        checkState(!((boolean) f.get(sqlSession)), "Autocommit must be false");
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      LOG.debug("Failed to check the autocommit status of SqlSession", e);
    }
  }

  @Override
  public void enableCaching() {
    LOG.trace("{} enabled caching", currentThread());
    CACHING_ENABLED.set(Boolean.TRUE);
  }

  @Override
  public DbSession openSession(boolean batch) {
    LOG.trace("{} called openSession({}) (caching={})", currentThread(), batch, CACHING_ENABLED.get());
    if (!CACHING_ENABLED.get()) {
      DbSession res = myBatis.openSession(batch);
      LOG.trace("{} created non cached {} session (batch={})", currentThread(), res, batch);
      return res;
    }
    if (batch) {
      return new NonClosingDbSession(batchDbSession.get().get());
    }
    return new NonClosingDbSession(regularDbSession.get().get());
  }

  @Override
  public void disableCaching() {
    LOG.trace("{} disabled caching", currentThread());
    close(regularDbSession, "regular");
    close(batchDbSession, "batch");
    regularDbSession.remove();
    batchDbSession.remove();
    CACHING_ENABLED.remove();
  }

  public void close(ThreadLocal<DelegatingDbSessionSupplier> dbSessionThreadLocal, String label) {
    DelegatingDbSessionSupplier delegatingDbSessionSupplier = dbSessionThreadLocal.get();
    boolean getCalled = delegatingDbSessionSupplier.isPopulated();
    LOG.trace("{} attempts closing on {} session (getCalled={})", currentThread(), label, getCalled);
    if (getCalled) {
      try {
        DbSession res = delegatingDbSessionSupplier.get();
        LOG.trace("{} closes {}", currentThread(), res);
        res.close();
      } catch (Exception e) {
        LOG.error(format("Failed to close %s connection in %s", label, currentThread()), e);
      }
    }
  }

  /**
   * A {@link Supplier} of {@link DelegatingDbSession} which logs whether {@link Supplier#get() get} has been called at
   * least once, delegates the actual supplying to the a specific {@link Supplier<NonClosingDbSession>} instance and
   * caches the supplied {@link NonClosingDbSession}.
   */
  private static final class DelegatingDbSessionSupplier implements Supplier<DbSession> {
    private final Supplier<DbSession> delegate;
    private DbSession dbSession;

    DelegatingDbSessionSupplier(Supplier<DbSession> delegate) {
      this.delegate = delegate;
    }

    @Override
    public DbSession get() {
      if (dbSession == null) {
        dbSession = Objects.requireNonNull(delegate.get());
      }
      return dbSession;
    }

    boolean isPopulated() {
      return dbSession != null;
    }
  }

}
