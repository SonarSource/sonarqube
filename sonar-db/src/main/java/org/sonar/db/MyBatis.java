/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Nullable;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.activity.ActivityDto;
import org.sonar.db.activity.ActivityMapper;
import org.sonar.db.ce.CeActivityMapper;
import org.sonar.db.ce.CeQueueMapper;
import org.sonar.db.ce.CeScannerContextMapper;
import org.sonar.db.ce.CeTaskInputMapper;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.component.ComponentKeyUpdaterMapper;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.ComponentLinkMapper;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ResourceIndexDto;
import org.sonar.db.component.ResourceIndexMapper;
import org.sonar.db.component.ResourceMapper;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotMapper;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.db.component.ViewsSnapshotDto;
import org.sonar.db.dashboard.ActiveDashboardDto;
import org.sonar.db.dashboard.ActiveDashboardMapper;
import org.sonar.db.dashboard.DashboardDto;
import org.sonar.db.dashboard.DashboardMapper;
import org.sonar.db.dashboard.WidgetDto;
import org.sonar.db.dashboard.WidgetMapper;
import org.sonar.db.dashboard.WidgetPropertyDto;
import org.sonar.db.dashboard.WidgetPropertyMapper;
import org.sonar.db.debt.RequirementMigrationDto;
import org.sonar.db.duplication.DuplicationMapper;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventMapper;
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
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureFilterDto;
import org.sonar.db.measure.MeasureFilterFavouriteDto;
import org.sonar.db.measure.MeasureFilterFavouriteMapper;
import org.sonar.db.measure.MeasureFilterMapper;
import org.sonar.db.measure.MeasureMapper;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.measure.custom.CustomMeasureMapper;
import org.sonar.db.metric.MetricMapper;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.notification.NotificationQueueMapper;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicMapper;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateMapper;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.property.PropertiesMapper;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.db.purge.PurgeMapper;
import org.sonar.db.purge.PurgeableAnalysisDto;
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
import org.sonar.db.user.UserPermissionDto;
import org.sonar.db.user.UserTokenCount;
import org.sonar.db.user.UserTokenDto;
import org.sonar.db.user.UserTokenMapper;
import org.sonar.db.version.SchemaMigrationDto;
import org.sonar.db.version.SchemaMigrationMapper;
import org.sonar.db.version.v45.Migration45Mapper;
import org.sonar.db.version.v50.Migration50Mapper;
import org.sonar.db.version.v53.Migration53Mapper;

public class MyBatis {

  private final Database database;
  private SqlSessionFactory sessionFactory;

  public MyBatis(Database database) {
    this.database = database;
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

  public MyBatis start() {
    LogFactory.useSlf4jLogging();

    MyBatisConfBuilder confBuilder = new MyBatisConfBuilder(database);

    confBuilder.loadAlias("ActiveDashboard", ActiveDashboardDto.class);
    confBuilder.loadAlias("Author", AuthorDto.class);
    confBuilder.loadAlias("Component", ComponentDto.class);
    confBuilder.loadAlias("ComponentWithSnapshot", ComponentDtoWithSnapshotId.class);
    confBuilder.loadAlias("ComponentLink", ComponentLinkDto.class);
    confBuilder.loadAlias("Dashboard", DashboardDto.class);
    confBuilder.loadAlias("DuplicationUnit", DuplicationUnitDto.class);
    confBuilder.loadAlias("Group", GroupDto.class);
    confBuilder.loadAlias("GroupRole", GroupRoleDto.class);
    confBuilder.loadAlias("GroupMembership", GroupMembershipDto.class);
    confBuilder.loadAlias("LoadedTemplate", LoadedTemplateDto.class);
    confBuilder.loadAlias("MeasureFilter", MeasureFilterDto.class);
    confBuilder.loadAlias("MeasureFilterFavourite", MeasureFilterFavouriteDto.class);
    confBuilder.loadAlias("NotificationQueue", NotificationQueueDto.class);
    confBuilder.loadAlias("Property", PropertyDto.class);
    confBuilder.loadAlias("PurgeableAnalysis", PurgeableAnalysisDto.class);
    confBuilder.loadAlias("QualityGate", QualityGateDto.class);
    confBuilder.loadAlias("QualityGateCondition", QualityGateConditionDto.class);
    confBuilder.loadAlias("ProjectQgateAssociation", ProjectQgateAssociationDto.class);
    confBuilder.loadAlias("Resource", ResourceDto.class);
    confBuilder.loadAlias("ResourceIndex", ResourceIndexDto.class);
    confBuilder.loadAlias("Rule", RuleDto.class);
    confBuilder.loadAlias("RuleParam", RuleParamDto.class);
    confBuilder.loadAlias("Snapshot", SnapshotDto.class);
    confBuilder.loadAlias("SchemaMigration", SchemaMigrationDto.class);
    confBuilder.loadAlias("User", UserDto.class);
    confBuilder.loadAlias("UserRole", UserPermissionDto.class);
    confBuilder.loadAlias("UserGroup", UserGroupDto.class);
    confBuilder.loadAlias("Widget", WidgetDto.class);
    confBuilder.loadAlias("WidgetProperty", WidgetPropertyDto.class);
    confBuilder.loadAlias("Measure", MeasureDto.class);
    confBuilder.loadAlias("Issue", IssueDto.class);
    confBuilder.loadAlias("IssueChange", IssueChangeDto.class);
    confBuilder.loadAlias("IssueFilter", IssueFilterDto.class);
    confBuilder.loadAlias("IssueFilterFavourite", IssueFilterFavouriteDto.class);
    confBuilder.loadAlias("PermissionTemplate", PermissionTemplateDto.class);
    confBuilder.loadAlias("PermissionTemplateUser", PermissionTemplateUserDto.class);
    confBuilder.loadAlias("PermissionTemplateGroup", PermissionTemplateGroupDto.class);
    confBuilder.loadAlias("PermissionTemplateCharacteristic", PermissionTemplateCharacteristicDto.class);
    confBuilder.loadAlias("UserWithPermission", UserWithPermissionDto.class);
    confBuilder.loadAlias("GroupWithPermission", GroupWithPermissionDto.class);
    confBuilder.loadAlias("QualityProfile", QualityProfileDto.class);
    confBuilder.loadAlias("ActiveRule", ActiveRuleDto.class);
    confBuilder.loadAlias("ActiveRuleParam", ActiveRuleParamDto.class);
    confBuilder.loadAlias("RequirementMigration", RequirementMigrationDto.class);
    confBuilder.loadAlias("Activity", ActivityDto.class);
    confBuilder.loadAlias("IdUuidPair", IdUuidPair.class);
    confBuilder.loadAlias("FilePathWithHash", FilePathWithHashDto.class);
    confBuilder.loadAlias("UuidWithProjectUuid", UuidWithProjectUuidDto.class);
    confBuilder.loadAlias("Event", EventDto.class);
    confBuilder.loadAlias("CustomMeasure", CustomMeasureDto.class);
    confBuilder.loadAlias("ViewsSnapshot", ViewsSnapshotDto.class);
    confBuilder.loadAlias("UserToken", UserTokenDto.class);
    confBuilder.loadAlias("UserTokenCount", UserTokenCount.class);

    // AuthorizationMapper has to be loaded before IssueMapper because this last one used it
    confBuilder.loadMapper("org.sonar.db.user.AuthorizationMapper");
    // ResourceMapper has to be loaded before IssueMapper because this last one used it
    confBuilder.loadMapper(ResourceMapper.class);

    confBuilder.loadMapper("org.sonar.db.permission.PermissionMapper");
    Class<?>[] mappers = {ActivityMapper.class, ActiveDashboardMapper.class, AuthorMapper.class, DashboardMapper.class,
      DuplicationMapper.class,
      IssueMapper.class, IssueChangeMapper.class, IssueFilterMapper.class, IssueFilterFavouriteMapper.class,
      IsAliveMapper.class,
      LoadedTemplateMapper.class, MeasureFilterMapper.class, MeasureFilterFavouriteMapper.class,
      PermissionTemplateMapper.class, PermissionTemplateCharacteristicMapper.class,
      PropertiesMapper.class, PurgeMapper.class, ComponentKeyUpdaterMapper.class, ResourceIndexMapper.class, RoleMapper.class, RuleMapper.class,
      SchemaMigrationMapper.class, WidgetMapper.class, WidgetPropertyMapper.class,
      UserMapper.class, GroupMapper.class, UserGroupMapper.class, UserTokenMapper.class,
      FileSourceMapper.class,
      NotificationQueueMapper.class,
      GroupMembershipMapper.class, QualityProfileMapper.class, ActiveRuleMapper.class,
      MeasureMapper.class, MetricMapper.class, CustomMeasureMapper.class, QualityGateMapper.class, QualityGateConditionMapper.class, ComponentMapper.class, SnapshotMapper.class,
      ProjectQgateAssociationMapper.class, EventMapper.class,
      CeQueueMapper.class, CeActivityMapper.class, CeTaskInputMapper.class, CeScannerContextMapper.class,
      ComponentLinkMapper.class,
      Migration45Mapper.class, Migration50Mapper.class, Migration53Mapper.class
    };
    confBuilder.loadMappers(mappers);

    sessionFactory = new SqlSessionFactoryBuilder().build(confBuilder.build());
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
      return new BatchSession(session);
    }
    SqlSession session = sessionFactory.openSession(ExecutorType.REUSE);
    return new DbSession(session);
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
