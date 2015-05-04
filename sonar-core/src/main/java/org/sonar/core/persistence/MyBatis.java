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

import java.io.InputStream;

import javax.annotation.Nullable;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
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
import org.sonar.core.component.ComponentLinkDto;
import org.sonar.core.component.FilePathWithHashDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.UuidWithProjectUuidDto;
import org.sonar.core.component.db.ComponentIndexMapper;
import org.sonar.core.component.db.ComponentLinkMapper;
import org.sonar.core.component.db.ComponentMapper;
import org.sonar.core.component.db.SnapshotMapper;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
import org.sonar.core.config.Logback;
import org.sonar.core.dashboard.ActiveDashboardDto;
import org.sonar.core.dashboard.ActiveDashboardMapper;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.core.dashboard.DashboardMapper;
import org.sonar.core.dashboard.WidgetDto;
import org.sonar.core.dashboard.WidgetMapper;
import org.sonar.core.dashboard.WidgetPropertyDto;
import org.sonar.core.dashboard.WidgetPropertyMapper;
import org.sonar.core.design.FileDependencyMapper;
import org.sonar.core.duplication.DuplicationMapper;
import org.sonar.core.duplication.DuplicationUnitDto;
import org.sonar.core.event.EventDto;
import org.sonar.core.event.db.EventMapper;
import org.sonar.core.graph.jdbc.GraphDto;
import org.sonar.core.graph.jdbc.GraphDtoMapper;
import org.sonar.core.issue.db.ActionPlanDto;
import org.sonar.core.issue.db.ActionPlanMapper;
import org.sonar.core.issue.db.ActionPlanStatsDto;
import org.sonar.core.issue.db.ActionPlanStatsMapper;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueChangeMapper;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.issue.db.IssueFilterFavouriteDto;
import org.sonar.core.issue.db.IssueFilterFavouriteMapper;
import org.sonar.core.issue.db.IssueFilterMapper;
import org.sonar.core.issue.db.IssueMapper;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureFilterDto;
import org.sonar.core.measure.db.MeasureFilterMapper;
import org.sonar.core.measure.db.MeasureMapper;
import org.sonar.core.measure.db.MetricMapper;
import org.sonar.core.notification.db.NotificationQueueDto;
import org.sonar.core.notification.db.NotificationQueueMapper;
import org.sonar.core.permission.GroupWithPermissionDto;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.permission.PermissionTemplateGroupDto;
import org.sonar.core.permission.PermissionTemplateMapper;
import org.sonar.core.permission.PermissionTemplateUserDto;
import org.sonar.core.permission.UserWithPermissionDto;
import org.sonar.core.persistence.dialect.Dialect;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.core.persistence.migration.v45.Migration45Mapper;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.core.properties.PropertiesMapper;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeMapper;
import org.sonar.core.purge.PurgeableSnapshotDto;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationDto;
import org.sonar.core.qualitygate.db.ProjectQgateAssociationMapper;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateConditionMapper;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.core.qualitygate.db.QualityGateMapper;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleMapper;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileMapper;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceIndexDto;
import org.sonar.core.resource.ResourceIndexerMapper;
import org.sonar.core.resource.ResourceKeyUpdaterMapper;
import org.sonar.core.resource.ResourceMapper;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleMapper;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.source.db.FileSourceMapper;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.core.technicaldebt.db.CharacteristicMapper;
import org.sonar.core.technicaldebt.db.RequirementMigrationDto;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.template.LoadedTemplateMapper;
import org.sonar.core.user.AuthorDto;
import org.sonar.core.user.AuthorMapper;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.GroupMapper;
import org.sonar.core.user.GroupMembershipDto;
import org.sonar.core.user.GroupMembershipMapper;
import org.sonar.core.user.GroupRoleDto;
import org.sonar.core.user.RoleMapper;
import org.sonar.core.user.UserDto;
import org.sonar.core.user.UserGroupDto;
import org.sonar.core.user.UserGroupMapper;
import org.sonar.core.user.UserMapper;
import org.sonar.core.user.UserRoleDto;

import ch.qos.logback.classic.Level;

import com.google.common.io.Closeables;

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
    loadAlias(conf, "ComponentLink", ComponentLinkDto.class);
    loadAlias(conf, "Dashboard", DashboardDto.class);
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
    loadAlias(conf, "Issue", IssueDto.class);
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
    loadAlias(conf, "Event", EventDto.class);

    // AuthorizationMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, "org.sonar.core.user.AuthorizationMapper");
    // ResourceMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, ResourceMapper.class);

    loadMapper(conf, "org.sonar.core.permission.PermissionMapper");
    Class<?>[] mappers = {ActivityMapper.class, ActiveDashboardMapper.class, AuthorMapper.class, DashboardMapper.class,
      FileDependencyMapper.class, DuplicationMapper.class, GraphDtoMapper.class,
      IssueMapper.class, IssueChangeMapper.class, IssueFilterMapper.class, IssueFilterFavouriteMapper.class,
      IsAliveMapper.class,
      LoadedTemplateMapper.class, MeasureFilterMapper.class, Migration44Mapper.class, PermissionTemplateMapper.class, PropertiesMapper.class, PurgeMapper.class,
      ResourceKeyUpdaterMapper.class, ResourceIndexerMapper.class, RoleMapper.class, RuleMapper.class,
      SchemaMigrationMapper.class, SemaphoreMapper.class, UserMapper.class, GroupMapper.class, UserGroupMapper.class, WidgetMapper.class, WidgetPropertyMapper.class,
      FileSourceMapper.class, ActionPlanMapper.class,
      ActionPlanStatsMapper.class,
      NotificationQueueMapper.class, CharacteristicMapper.class,
      GroupMembershipMapper.class, QualityProfileMapper.class, ActiveRuleMapper.class,
      MeasureMapper.class, MetricMapper.class, QualityGateMapper.class, QualityGateConditionMapper.class, ComponentMapper.class, SnapshotMapper.class,
      ProjectQgateAssociationMapper.class, EventMapper.class,
      AnalysisReportMapper.class, ComponentIndexMapper.class, ComponentLinkMapper.class,
      Migration45Mapper.class, Migration50Mapper.class
    };
    loadMappers(conf, mappers);

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

  private void loadMapper(Configuration configuration, Class mapperClass) {
    loadMapper(configuration, mapperClass.getName());
  }

  private void loadMapper(Configuration configuration, String mapperName) {
    InputStream input = null;
    try {
      input = getClass().getResourceAsStream("/" + mapperName.replace('.', '/') + ".xml");
      new XMLMapperBuilder(input, configuration, mapperName, configuration.getSqlFragments()).parse();
      configuration.addLoadedResource(mapperName);
      logback.setLoggerLevel(mapperName, Level.INFO);
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
