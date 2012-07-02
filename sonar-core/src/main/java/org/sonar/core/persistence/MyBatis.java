/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.dashboard.*;
import org.sonar.core.dependency.DependencyDto;
import org.sonar.core.dependency.DependencyMapper;
import org.sonar.core.dependency.ResourceSnapshotDto;
import org.sonar.core.dependency.ResourceSnapshotMapper;
import org.sonar.core.duplication.DuplicationMapper;
import org.sonar.core.duplication.DuplicationUnitDto;
import org.sonar.core.filter.*;
import org.sonar.core.properties.PropertiesMapper;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.purge.PurgeMapper;
import org.sonar.core.purge.PurgeVendorMapper;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.core.resource.*;
import org.sonar.core.review.ReviewCommentDto;
import org.sonar.core.review.ReviewCommentMapper;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewMapper;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.template.LoadedTemplateMapper;
import org.sonar.core.user.*;

import java.io.InputStream;

public class MyBatis implements BatchComponent, ServerComponent {

  private final Database database;
  private SqlSessionFactory sessionFactory;

  public MyBatis(Database database) {
    this.database = database;
  }

  public MyBatis start() {
    LogFactory.useSlf4jLogging();
    Configuration conf = new Configuration();
    conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    conf.setUseGeneratedKeys(true);
    conf.setLazyLoadingEnabled(false);
    conf.getVariables().setProperty("_true", database.getDialect().getTrueSqlValue());
    conf.getVariables().setProperty("_false", database.getDialect().getFalseSqlValue());

    loadAlias(conf, "ActiveDashboard", ActiveDashboardDto.class);
    loadAlias(conf, "Author", AuthorDto.class);
    loadAlias(conf, "Filter", FilterDto.class);
    loadAlias(conf, "Criterion", CriterionDto.class);
    loadAlias(conf, "FilterColumn", FilterColumnDto.class);
    loadAlias(conf, "Dashboard", DashboardDto.class);
    loadAlias(conf, "Dependency", DependencyDto.class);
    loadAlias(conf, "DuplicationUnit", DuplicationUnitDto.class);
    loadAlias(conf, "Group", GroupDto.class);
    loadAlias(conf, "GroupRole", GroupRoleDto.class);
    loadAlias(conf, "LoadedTemplate", LoadedTemplateDto.class);
    loadAlias(conf, "Property", PropertyDto.class);
    loadAlias(conf, "PurgeableSnapshot", PurgeableSnapshotDto.class);
    loadAlias(conf, "Resource", ResourceDto.class);
    loadAlias(conf, "ResourceIndex", ResourceIndexDto.class);
    loadAlias(conf, "ResourceSnapshot", ResourceSnapshotDto.class);
    loadAlias(conf, "Review", ReviewDto.class);
    loadAlias(conf, "ReviewComment", ReviewCommentDto.class);
    loadAlias(conf, "Rule", RuleDto.class);
    loadAlias(conf, "Snapshot", SnapshotDto.class);
    loadAlias(conf, "SchemaMigration", SchemaMigrationDto.class);
    loadAlias(conf, "User", UserDto.class);
    loadAlias(conf, "UserRole", UserRoleDto.class);
    loadAlias(conf, "Widget", WidgetDto.class);
    loadAlias(conf, "WidgetProperty", WidgetPropertyDto.class);

    loadMapper(conf, ActiveDashboardMapper.class);
    loadMapper(conf, AuthorMapper.class);
    loadMapper(conf, FilterMapper.class);
    loadMapper(conf, CriterionMapper.class);
    loadMapper(conf, FilterColumnMapper.class);
    loadMapper(conf, DashboardMapper.class);
    loadMapper(conf, DependencyMapper.class);
    loadMapper(conf, DuplicationMapper.class);
    loadMapper(conf, LoadedTemplateMapper.class);
    loadMapper(conf, PropertiesMapper.class);
    loadMapper(conf, PurgeMapper.class);
    loadMapper(conf, PurgeVendorMapper.class);
    loadMapper(conf, ResourceKeyUpdaterMapper.class);
    loadMapper(conf, ResourceIndexerMapper.class);
    loadMapper(conf, ResourceMapper.class);
    loadMapper(conf, ResourceSnapshotMapper.class);
    loadMapper(conf, ReviewCommentMapper.class);
    loadMapper(conf, ReviewMapper.class);
    loadMapper(conf, RoleMapper.class);
    loadMapper(conf, RuleMapper.class);
    loadMapper(conf, SchemaMigrationMapper.class);
    loadMapper(conf, UserMapper.class);
    loadMapper(conf, WidgetMapper.class);
    loadMapper(conf, WidgetPropertyMapper.class);

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

  private void loadMapper(Configuration conf, Class mapperClass) {
    // trick to use database-specific XML files for a single Mapper Java interface
    InputStream input = getPathToMapper(mapperClass);
    try {
      XMLMapperBuilder mapperParser = new XMLMapperBuilder(input, conf, mapperClass.getName(), conf.getSqlFragments());
      mapperParser.parse();
      conf.addLoadedResource(mapperClass.getName());

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private InputStream getPathToMapper(Class mapperClass) {
    InputStream input = getClass().getResourceAsStream(
      "/" + StringUtils.replace(mapperClass.getName(), ".", "/") + "-" + database.getDialect().getId() + ".xml");
    if (input == null) {
      input = getClass().getResourceAsStream("/" + StringUtils.replace(mapperClass.getName(), ".", "/") + ".xml");
    }
    return input;
  }

  private void loadAlias(Configuration conf, String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }

}
