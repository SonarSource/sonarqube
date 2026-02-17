/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.annotations.VisibleForTesting;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.sonar.db.common.Common;

/**
 * This is a base class for database configurations to implement {@link MyBatis}.
 * It provides common configuration and utilities for MyBatis.
 */
public abstract class AbstractMyBatis implements MyBatis {
  private final List<BaseMyBatisConfExtension> confExtensions;
  private final Database database;
  private SqlSessionFactory sessionFactory;

  protected AbstractMyBatis(Database database) {
    this(database, List.of());
  }

  protected AbstractMyBatis(Database database, @Nullable BaseMyBatisConfExtension[] confExtensions) {
    this.confExtensions = confExtensions == null ? Collections.emptyList() : Arrays.asList(confExtensions);
    this.database = database;
  }

  protected AbstractMyBatis(Database database, Collection<BaseMyBatisConfExtension> confExtensions) {
    this.confExtensions = List.copyOf(confExtensions);
    this.database = database;
  }

  /**
   * Override this to set up aliases for example, otherwise mappers may fail to load.
   * Called during the start() method after loading any aliases from extensions passed
   * to the constructor, but before loading any mappers.
   */
  protected void configureBeforeMappersOnStart(MyBatisConfBuilder confBuilder) {

  }

  /**
   * Override this to configure more things after adding mappers; this may add yet more mappers.
   * Called during the start() method after loading mappers from extensions passed to the constructor,
   * but before building the session factory.
   */
  protected void configureAfterMappersOnStart(MyBatisConfBuilder confBuilder) {

  }

  public void start() {
    LogFactory.useNoLogging();

    MyBatisConfBuilder confBuilder = new MyBatisConfBuilder(database);

    confExtensions.forEach(ext -> ext.loadAliases(confBuilder::loadAlias));

    configureBeforeMappersOnStart(confBuilder);

    // keep them sorted alphabetically
    Class<?>[] mappers = {
      Common.class,
      IsAliveMapper.class
    };

    confBuilder.loadMappers(mappers);
    confExtensions.stream()
      .flatMap(BaseMyBatisConfExtension::getMapperClasses)
      .forEach(confBuilder::loadMapper);

    configureAfterMappersOnStart(confBuilder);

    sessionFactory = new SqlSessionFactoryBuilder().build(confBuilder.build());
  }

  @VisibleForTesting
  SqlSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  protected Database getDatabase() {
    return database;
  }

  /**
   * Opens a new session from the MyBatis session factory. If batch is true,
   * the session will automatically commit the transaction after a certain number of insert/update/delete
   * statements have been executed.
   */
  @Override
  public DbSession openSession(boolean batch) {
    if (batch) {
      SqlSession session = sessionFactory.openSession(ExecutorType.BATCH, TransactionIsolationLevel.READ_COMMITTED);
      return new BatchSession(session);
    }
    SqlSession session = sessionFactory.openSession(ExecutorType.REUSE, TransactionIsolationLevel.READ_COMMITTED);
    return new DbSessionImpl(session);
  }

  /**
   * Creates a scrolling PreparedStatement with the fetch size configured based on what's appropriate
   * for the database dialect.
   */
  @Override
  public PreparedStatement newScrollingSelectStatement(DbSession session, String sql) {
    int fetchSize = database.getDialect().getScrollDefaultFetchSize();
    return newScrollingSelectStatement(session, sql, fetchSize);
  }

  private static PreparedStatement newScrollingSelectStatement(DbSession session, String sql, int fetchSize) {
    try {
      PreparedStatement stmt = session.getConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(fetchSize);
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to create SQL statement: " + sql, e);
    }
  }
}
