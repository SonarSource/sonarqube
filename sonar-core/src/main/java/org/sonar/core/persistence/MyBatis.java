/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
import org.sonar.api.database.model.MeasureModel;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.activity.db.ActivityMapper;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.UuidWithProjectUuidDto;
import org.sonar.core.component.db.ComponentIndexMapper;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.component.db.SnapshotMapper;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
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
import org.sonar.core.issue.db.*;
import org.sonar.core.measure.db.*;
import org.sonar.core.notification.db.NotificationQueueDto;
import org.sonar.core.notification.db.NotificationQueueMapper;
import org.sonar.core.permission.*;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.core.persistence.migration.v45.Migration45Mapper;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.core.properties.PropertiesMapper;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeMapper;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.core.qualitygate.db.*;
import org.sonar.core.qualityprofile.db.*;
import org.sonar.core.resource.*;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.source.db.FileSourceMapper;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.core.technicaldebt.db.CharacteristicMapper;
import org.sonar.core.technicaldebt.db.RequirementMigrationDto;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.template.LoadedTemplateMapper;
import org.sonar.core.user.*;

import javax.annotation.Nullable;

import java.io.InputStream;

public class MyBatis implements BatchComponent, ServerComponent {

  private final Database database;
  private final Logback logback;
  private SqlSessionFactory sessionFactory;

  // TODO this queue should directly be an IndexQueue. Pending move of persistence to sonar-server
  private WorkQueue queue;

  public MyBatis(Database database, Logback logback, WorkQueue queue) {
    this.database = database;
    this.logback = logback;
    this.queue = queue;
  }

  public static void closeQuietly(@Nullable SqlSession session) {
    if (session != null) {
      try {
        session.close();
      } catch (Exception e) {
        LoggerFactory.getLogger(MyBatis.class).warn("Fail to close session", e);
        // do not re-throw the exception
      }
    }
  }

  private static JdbcTransactionFactory createTransactionFactory() {
    return new JdbcTransactionFactory();
  }

  public MyBatis start() {
    LogFactory.useSlf4jLogging();

    Configuration conf = new Configuration();
    conf.setEnvironment(new Environment("production", createTransactionFactory(), database.getDataSource()));
    conf.setUseGeneratedKeys(true);
    conf.setLazyLoadingEnabled(false);
    conf.setJdbcTypeForNull(JdbcType.NULL);
    Dialect dialect = database.getDialect();
    conf.setDatabaseId(dialect.getId());
    conf.getVariables().setProperty("_true", dialect.getTrueSqlValue());
    conf.getVariables().setProperty("_false", dialect.getFalseSqlValue());
    conf.getVariables().setProperty("_scrollFetchSize", String.valueOf(dialect.getScrollDefaultFetchSize()));

    loadAlias(conf, "ActiveDashboard", ActiveDashboardDto.class);
    loadAlias(conf, "Author", AuthorDto.class);
    loadAlias(conf, "Component", ComponentDto.class);
    loadAlias(conf, "Dashboard", DashboardDto.class);
    loadAlias(conf, "Dependency", DependencyDto.class);
    loadAlias(conf, "DuplicationUnit", DuplicationUnitDto.class);
    loadAlias(conf, "Graph", GraphDto.class);
    loadAlias(conf, "Group", GroupDto.class);
    loadAlias(conf, "GroupRole", GroupRoleDto.class);
    loadAlias(conf, "GroupMembership", GroupMembershipDto.class);
    loadAlias(conf, "LoadedTemplate", LoadedTemplateDto.class);
    loadAlias(conf, "MeasureFilter", MeasureFilterDto.class);
    loadAlias(conf, "NotificationQueue", NotificationQueueDto.class);
    loadAlias(conf, "Property", PropertyDto.class);
    loadAlias(conf, "PurgeableSnapshot", PurgeableSnapshotDto.class);
    loadAlias(conf, "QualityGate", QualityGateDto.class);
    loadAlias(conf, "QualityGateCondition", QualityGateConditionDto.class);
    loadAlias(conf, "ProjectQgateAssociation", ProjectQgateAssociationDto.class);
    loadAlias(conf, "Resource", ResourceDto.class);
    loadAlias(conf, "ResourceIndex", ResourceIndexDto.class);
    loadAlias(conf, "ResourceSnapshot", ResourceSnapshotDto.class);
    loadAlias(conf, "Rule", RuleDto.class);
    loadAlias(conf, "RuleParam", RuleParamDto.class);
    loadAlias(conf, "Snapshot", SnapshotDto.class);
    loadAlias(conf, "Semaphore", SemaphoreDto.class);
    loadAlias(conf, "SchemaMigration", SchemaMigrationDto.class);
    loadAlias(conf, "User", UserDto.class);
    loadAlias(conf, "UserRole", UserRoleDto.class);
    loadAlias(conf, "UserGroup", UserGroupDto.class);
    loadAlias(conf, "Widget", WidgetDto.class);
    loadAlias(conf, "WidgetProperty", WidgetPropertyDto.class);
    loadAlias(conf, "MeasureModel", MeasureModel.class);
    loadAlias(conf, "Measure", MeasureDto.class);
    loadAlias(conf, "Metric", MetricDto.class);
    loadAlias(conf, "Issue", IssueDto.class);
    loadAlias(conf, "BatchIssue", BatchIssueDto.class);
    loadAlias(conf, "IssueChange", IssueChangeDto.class);
    loadAlias(conf, "IssueFilter", IssueFilterDto.class);
    loadAlias(conf, "IssueFilterFavourite", IssueFilterFavouriteDto.class);
    loadAlias(conf, "ActionPlanIssue", ActionPlanDto.class);
    loadAlias(conf, "ActionPlanStats", ActionPlanStatsDto.class);
    loadAlias(conf, "PermissionTemplate", PermissionTemplateDto.class);
    loadAlias(conf, "PermissionTemplateUser", PermissionTemplateUserDto.class);
    loadAlias(conf, "PermissionTemplateGroup", PermissionTemplateGroupDto.class);
    loadAlias(conf, "Characteristic", CharacteristicDto.class);
    loadAlias(conf, "UserWithPermission", UserWithPermissionDto.class);
    loadAlias(conf, "GroupWithPermission", GroupWithPermissionDto.class);
    loadAlias(conf, "QualityProfile", QualityProfileDto.class);
    loadAlias(conf, "ActiveRule", ActiveRuleDto.class);
    loadAlias(conf, "ActiveRuleParam", ActiveRuleParamDto.class);
    loadAlias(conf, "RequirementMigration", RequirementMigrationDto.class);
    loadAlias(conf, "Activity", ActivityDto.class);
    loadAlias(conf, "AnalysisReport", AnalysisReportDto.class);
    loadAlias(conf, "IdUuidPair", IdUuidPair.class);
    loadAlias(conf, "FilePathWithHash", FilePathWithHashDto.class);
    loadAlias(conf, "UuidWithProjectUuid", UuidWithProjectUuidDto.class);

    // AuthorizationMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, "org.sonar.core.user.AuthorizationMapper");
    // ResourceMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, ResourceMapper.class);

    loadMapper(conf, "org.sonar.core.permission.PermissionMapper");
    Class<?>[] mappers = {ActivityMapper.class, ActiveDashboardMapper.class, AuthorMapper.class, DashboardMapper.class,
      DependencyMapper.class, DuplicationMapper.class, GraphDtoMapper.class,
      IssueMapper.class, IssueChangeMapper.class, IssueFilterMapper.class, IssueFilterFavouriteMapper.class,
      LoadedTemplateMapper.class, MeasureFilterMapper.class, Migration44Mapper.class, PermissionTemplateMapper.class, PropertiesMapper.class, PurgeMapper.class,
      ResourceKeyUpdaterMapper.class, ResourceIndexerMapper.class, ResourceSnapshotMapper.class, RoleMapper.class, RuleMapper.class,
      SchemaMigrationMapper.class, SemaphoreMapper.class, UserMapper.class, GroupMapper.class, UserGroupMapper.class, WidgetMapper.class, WidgetPropertyMapper.class,
      org.sonar.api.database.model.MeasureMapper.class, FileSourceMapper.class, ActionPlanMapper.class,
      ActionPlanStatsMapper.class,
      NotificationQueueMapper.class, CharacteristicMapper.class,
      GroupMembershipMapper.class, QualityProfileMapper.class, ActiveRuleMapper.class,
      MeasureMapper.class, MetricMapper.class, QualityGateMapper.class, QualityGateConditionMapper.class, ComponentMapper.class, SnapshotMapper.class,
      ProjectQgateAssociationMapper.class,
      AnalysisReportMapper.class, ComponentIndexMapper.class,
      Migration45Mapper.class, Migration50Mapper.class
    };
    loadMappers(conf, mappers);
    configureLogback(mappers);

    sessionFactory = new SqlSessionFactoryBuilder().build(conf);
    return this;
  }

  public SqlSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  /**
   * @deprecated since 4.4. Replaced by <code>openSession(false)</code>.
   */
  @Deprecated
  public SqlSession openSession() {
    return openSession(false);
  }

  /**
   * @deprecated since 4.4. Replaced by <code>openSession(true)</code>.
   */
  @Deprecated
  public BatchSession openBatchSession() {
    return (BatchSession) openSession(true);
  }

  /**
   * @since 4.4
   */
  public DbSession openSession(boolean batch) {
    if (batch) {
      SqlSession session = sessionFactory.openSession(ExecutorType.BATCH);
      return new BatchSession(queue, session);
    }
    SqlSession session = sessionFactory.openSession(ExecutorType.REUSE);
    return new DbSession(queue, session);
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
    for (Class mapperClass : mapperClasses) {
      logback.setLoggerLevel(mapperClass.getName(), Level.INFO);
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
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to load mapper " + mapperName, e);
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  private void loadAlias(Configuration conf, String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }
}
