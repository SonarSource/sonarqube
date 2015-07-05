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

package org.sonar.db;

import ch.qos.logback.classic.Level;
import com.google.common.io.Closeables;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.activity.ActivityDto;
import org.sonar.db.activity.ActivityMapper;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.ComponentLinkMapper;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceIndexDto;
import org.sonar.db.component.ResourceIndexMapper;
import org.sonar.db.component.ResourceKeyUpdaterMapper;
import org.sonar.db.component.ResourceMapper;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotMapper;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.db.compute.AnalysisReportDto;
import org.sonar.db.compute.AnalysisReportMapper;
import org.sonar.db.dashboard.ActiveDashboardDto;
import org.sonar.db.dashboard.ActiveDashboardMapper;
import org.sonar.db.dashboard.DashboardDto;
import org.sonar.db.dashboard.DashboardMapper;
import org.sonar.db.dashboard.WidgetDto;
import org.sonar.db.dashboard.WidgetMapper;
import org.sonar.db.dashboard.WidgetPropertyDto;
import org.sonar.db.dashboard.WidgetPropertyMapper;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.db.debt.CharacteristicMapper;
import org.sonar.db.debt.RequirementMigrationDto;
import org.sonar.db.deprecated.WorkQueue;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.duplication.DuplicationMapper;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventMapper;
import org.sonar.db.issue.ActionPlanDto;
import org.sonar.db.issue.ActionPlanMapper;
import org.sonar.db.issue.ActionPlanStatsDto;
import org.sonar.db.issue.ActionPlanStatsMapper;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.db.issue.IssueFilterFavouriteDto;
import org.sonar.db.issue.IssueFilterFavouriteMapper;
import org.sonar.db.issue.IssueFilterMapper;
import org.sonar.db.issue.IssueMapper;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.loadedtemplate.LoadedTemplateMapper;
import org.sonar.db.measure.CustomMeasureDto;
import org.sonar.db.measure.CustomMeasureMapper;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.db.measure.MeasureFilterMapper;
import org.sonar.db.measure.MeasureMapper;
import org.sonar.db.metric.MetricMapper;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.notification.NotificationQueueMapper;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.PermissionTemplateGroupDto;
import org.sonar.db.permission.PermissionTemplateMapper;
import org.sonar.db.permission.PermissionTemplateUserDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.property.PropertiesMapper;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.db.purge.PurgeMapper;
import org.sonar.db.purge.PurgeableSnapshotDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationMapper;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateConditionMapper;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.qualitygate.QualityGateMapper;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleMapper;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileMapper;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMapper;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.semaphore.SemaphoreDto;
import org.sonar.db.semaphore.SemaphoreMapper;
import org.sonar.db.source.FileSourceMapper;
import org.sonar.db.user.AuthorDto;
import org.sonar.db.user.AuthorMapper;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMapper;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipMapper;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.RoleMapper;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserGroupMapper;
import org.sonar.db.user.UserMapper;
import org.sonar.db.user.UserRoleDto;
import org.sonar.db.version.SchemaMigrationDto;
import org.sonar.db.version.SchemaMigrationMapper;
import org.sonar.db.version.v44.Migration44Mapper;
import org.sonar.db.version.v45.Migration45Mapper;
import org.sonar.db.version.v50.Migration50Mapper;

public class MyBatis {

  private final Database database;
  private SqlSessionFactory sessionFactory;
  private WorkQueue<?> queue;

  public MyBatis(Database database, WorkQueue<?> queue) {
    this.database = database;
    this.queue = queue;
  }

  // FIXME should be visible only to DAOs -> to be moved to AbstractDao
  public static void closeQuietly(@Nullable SqlSession session) {
    if (session != null) {
      try {
        session.close();
      } catch (Exception e) {
        Loggers.get(MyBatis.class).warn("Fail to close db session", e);
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
    loadAlias(conf, "CustomMeasure", CustomMeasureDto.class);

    // AuthorizationMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, "org.sonar.db.user.AuthorizationMapper");
    // ResourceMapper has to be loaded before IssueMapper because this last one used it
    loadMapper(conf, ResourceMapper.class);

    loadMapper(conf, "org.sonar.db.permission.PermissionMapper");
    Class<?>[] mappers = {ActivityMapper.class, ActiveDashboardMapper.class, AuthorMapper.class, DashboardMapper.class,
      DuplicationMapper.class,
      IssueMapper.class, IssueChangeMapper.class, IssueFilterMapper.class, IssueFilterFavouriteMapper.class,
      IsAliveMapper.class,
      LoadedTemplateMapper.class, MeasureFilterMapper.class, Migration44Mapper.class, PermissionTemplateMapper.class, PropertiesMapper.class, PurgeMapper.class,
      ResourceKeyUpdaterMapper.class, ResourceIndexMapper.class, RoleMapper.class, RuleMapper.class,
      SchemaMigrationMapper.class, SemaphoreMapper.class, UserMapper.class, GroupMapper.class, UserGroupMapper.class, WidgetMapper.class, WidgetPropertyMapper.class,
      FileSourceMapper.class, ActionPlanMapper.class,
      ActionPlanStatsMapper.class,
      NotificationQueueMapper.class, CharacteristicMapper.class,
      GroupMembershipMapper.class, QualityProfileMapper.class, ActiveRuleMapper.class,
      MeasureMapper.class, MetricMapper.class, CustomMeasureMapper.class, QualityGateMapper.class, QualityGateConditionMapper.class, ComponentMapper.class, SnapshotMapper.class,
      ProjectQgateAssociationMapper.class, EventMapper.class,
      AnalysisReportMapper.class, ComponentLinkMapper.class,
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
      ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(mapperName)).setLevel(Level.INFO);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to load mapper " + mapperName, e);
    } finally {
      Closeables.closeQuietly(input);
    }
  }

  private void loadAlias(Configuration conf, String alias, Class dtoClass) {
    conf.getTypeAliasRegistry().registerAlias(alias, dtoClass);
  }

  /**
   * Create a PreparedStatement for SELECT requests with scrolling of results
   */
  public PreparedStatement newScrollingSelectStatement(DbSession session, String sql) {
    int fetchSize = database.getDialect().getScrollDefaultFetchSize();
    return newScrollingSelectStatement(session, sql, fetchSize);
  }

  /**
   * Create a PreparedStatement for SELECT requests with scrolling of results row by row (only one row
   * in memory at a time)
   */
  public PreparedStatement newScrollingSingleRowSelectStatement(DbSession session, String sql) {
    int fetchSize = database.getDialect().getScrollSingleRowFetchSize();
    return newScrollingSelectStatement(session, sql, fetchSize);
  }

  private PreparedStatement newScrollingSelectStatement(DbSession session, String sql, int fetchSize) {
    try {
      PreparedStatement stmt = session.getConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(fetchSize);
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to create SQL statement: " + sql, e);
    }
  }
}
