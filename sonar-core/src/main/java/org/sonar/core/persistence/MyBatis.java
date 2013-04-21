/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import ch.qos.logback.classic.Level;
import com.google.common.io.Closeables;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.MeasureData;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.core.config.Logback;
import org.sonar.core.dashboard.*;
import org.sonar.core.dependency.DependencyDto;
import org.sonar.core.dependency.DependencyMapper;
import org.sonar.core.dependency.ResourceSnapshotDto;
import org.sonar.core.dependency.ResourceSnapshotMapper;
import org.sonar.core.duplication.DuplicationMapper;
import org.sonar.core.duplication.DuplicationUnitDto;
import org.sonar.core.graph.jdbc.GraphDto;
import org.sonar.core.graph.jdbc.GraphDtoMapper;
import org.sonar.core.issue.IssueChangeDto;
import org.sonar.core.issue.IssueChangeMapper;
import org.sonar.core.issue.IssueDto;
import org.sonar.core.measure.MeasureFilterDto;
import org.sonar.core.measure.MeasureFilterMapper;
import org.sonar.core.properties.PropertiesMapper;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.purge.PurgeMapper;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.core.resource.*;
import org.sonar.core.review.ReviewCommentDto;
import org.sonar.core.review.ReviewCommentMapper;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewMapper;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.source.jdbc.SnapshotDataDto;
import org.sonar.core.source.jdbc.SnapshotDataMapper;
import org.sonar.core.source.jdbc.SnapshotSourceMapper;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.template.LoadedTemplateMapper;
import org.sonar.core.user.*;

import java.io.InputStream;

public class MyBatis implements BatchComponent, ServerComponent {

  private final Database database;
  private final Settings settings;
  private final Logback logback;
  private SqlSessionFactory sessionFactory;

  public MyBatis(Database database, Settings settings, Logback logback) {
    this.database = database;
    this.settings = settings;
    this.logback = logback;
  }

  public MyBatis start() {
    LogFactory.useSlf4jLogging();

    Configuration conf = new Configuration();
    conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    conf.setDatabaseId(database.getDialect().getId());
    conf.setUseGeneratedKeys(true);
    conf.setLazyLoadingEnabled(false);
    conf.setJdbcTypeForNull(JdbcType.NULL);
    conf.getVariables().setProperty("_true", database.getDialect().getTrueSqlValue());
    conf.getVariables().setProperty("_false", database.getDialect().getFalseSqlValue());

    loadAlias(conf, "ActiveDashboard", ActiveDashboardDto.class);
    loadAlias(conf, "Author", AuthorDto.class);
    loadAlias(conf, "Dashboard", DashboardDto.class);
    loadAlias(conf, "Dependency", DependencyDto.class);
    loadAlias(conf, "DuplicationUnit", DuplicationUnitDto.class);
    loadAlias(conf, "Graph", GraphDto.class);
    loadAlias(conf, "Group", GroupDto.class);
    loadAlias(conf, "GroupRole", GroupRoleDto.class);
    loadAlias(conf, "LoadedTemplate", LoadedTemplateDto.class);
    loadAlias(conf, "MeasureFilter", MeasureFilterDto.class);
    loadAlias(conf, "Property", PropertyDto.class);
    loadAlias(conf, "PurgeableSnapshot", PurgeableSnapshotDto.class);
    loadAlias(conf, "Resource", ResourceDto.class);
    loadAlias(conf, "ResourceIndex", ResourceIndexDto.class);
    loadAlias(conf, "ResourceSnapshot", ResourceSnapshotDto.class);
    loadAlias(conf, "Review", ReviewDto.class);
    loadAlias(conf, "ReviewComment", ReviewCommentDto.class);
    loadAlias(conf, "Rule", RuleDto.class);
    loadAlias(conf, "Snapshot", SnapshotDto.class);
    loadAlias(conf, "Semaphore", SemaphoreDto.class);
    loadAlias(conf, "SchemaMigration", SchemaMigrationDto.class);
    loadAlias(conf, "User", UserDto.class);
    loadAlias(conf, "UserRole", UserRoleDto.class);
    loadAlias(conf, "Widget", WidgetDto.class);
    loadAlias(conf, "WidgetProperty", WidgetPropertyDto.class);
    loadAlias(conf, "MeasureModel", MeasureModel.class);
    loadAlias(conf, "MeasureData", MeasureData.class);
    loadAlias(conf, "Issue", IssueDto.class);
    loadAlias(conf, "IssueChange", IssueChangeDto.class);
    loadAlias(conf, "SnapshotData", SnapshotDataDto.class);

    Class<?>[] mappers = {ActiveDashboardMapper.class, AuthorMapper.class, DashboardMapper.class,
        DependencyMapper.class, DuplicationMapper.class, GraphDtoMapper.class, IssueChangeMapper.class, LoadedTemplateMapper.class,
        MeasureFilterMapper.class, PropertiesMapper.class, PurgeMapper.class, ResourceKeyUpdaterMapper.class, ResourceIndexerMapper.class, ResourceMapper.class,
        ResourceSnapshotMapper.class, ReviewCommentMapper.class, ReviewMapper.class, RoleMapper.class, RuleMapper.class, SchemaMigrationMapper.class,
        SemaphoreMapper.class, UserMapper.class, WidgetMapper.class, WidgetPropertyMapper.class, MeasureMapper.class, SnapshotDataMapper.class,
        SnapshotSourceMapper.class
    };
    loadMappers(conf, mappers);
    loadMapper(conf, "org.sonar.core.issue.IssueMapper");
    loadMapper(conf, "org.sonar.core.user.AuthorizationMapper");
    configureLogback(mappers);

    sessionFactory = new SqlSessionFactoryBuilder().build(conf);
    return this;
  }

  public SqlSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public SqlSession openSession() {
    return sessionFactory.openSession(ExecutorType.REUSE);
  }

  public BatchSession openBatchSession() {
    SqlSession session = sessionFactory.openSession(ExecutorType.BATCH);
    return new BatchSession(session);
  }

  public static void closeQuietly(SqlSession session) {
    if (session != null) {
      try {
        session.close();
      } catch (Exception e) {
        LoggerFactory.getLogger(MyBatis.class).warn("Fail to close session", e);
        // do not re-throw the exception
      }
    }
  }

  private void loadMappers(Configuration mybatisConf, Class<?>... mapperClasses) {
    for (Class mapperClass : mapperClasses) {
      loadMapper(mybatisConf, mapperClass);
    }
  }

  /**
   * See http://www.mybatis.org/core/logging.html :
   */
  private void configureLogback(Class<?>... mapperClasses) {
    Level level = Level.INFO;
    if (settings.getBoolean("sonar.showSql")) {
      level = settings.getBoolean("sonar.showSqlResults") ? Level.TRACE : Level.DEBUG;
    }
    for (Class mapperClass : mapperClasses) {
      logback.setLoggerLevel(mapperClass.getName(), level);
    }
  }

  private void loadMapper(Configuration configuration, Class mapperClass) {
    loadMapper(configuration, mapperClass.getName());
  }

  private void loadMapper(Configuration configuration, String mapperName) {
      InputStream input = null;
      try {
        input = getClass().getResourceAsStream("/" + mapperName.replace('.', '/') + ".xml");
        new XMLMapperBuilder(input, configuration, mapperName, configuration.getSqlFragments()).parse();
        configuration.addLoadedResource(mapperName);
      } finally {
        Closeables.closeQuietly(input);
      }
    }

  private void loadAlias(Configuration conf, String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }
}
