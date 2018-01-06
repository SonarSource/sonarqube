/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.sonar.api.Startable;
import org.sonar.db.ce.CeActivityMapper;
import org.sonar.db.ce.CeQueueMapper;
import org.sonar.db.ce.CeScannerContextMapper;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskCharacteristicMapper;
import org.sonar.db.ce.CeTaskInputMapper;
import org.sonar.db.component.AnalysisPropertiesMapper;
import org.sonar.db.component.BranchMapper;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.component.ComponentKeyUpdaterMapper;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.db.component.ComponentLinkMapper;
import org.sonar.db.component.ComponentMapper;
import org.sonar.db.component.FilePathWithHashDto;
import org.sonar.db.component.KeyWithUuidDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.component.ScrapAnalysisPropertyDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotMapper;
import org.sonar.db.component.UuidWithProjectUuidDto;
import org.sonar.db.component.ViewsSnapshotDto;
import org.sonar.db.debt.RequirementMigrationDto;
import org.sonar.db.duplication.DuplicationMapper;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.db.es.EsQueueMapper;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventMapper;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;
import org.sonar.db.issue.ShortBranchIssueDto;
import org.sonar.db.measure.LiveMeasureMapper;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.MeasureMapper;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.measure.custom.CustomMeasureMapper;
import org.sonar.db.metric.MetricMapper;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.notification.NotificationQueueMapper;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMapper;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.organization.OrganizationMemberMapper;
import org.sonar.db.permission.AuthorizationMapper;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.GroupPermissionMapper;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.permission.UserPermissionMapper;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicMapper;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.permission.template.PermissionTemplateMapper;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.plugin.PluginDto;
import org.sonar.db.plugin.PluginMapper;
import org.sonar.db.property.InternalPropertiesMapper;
import org.sonar.db.property.InternalPropertyDto;
import org.sonar.db.property.PropertiesMapper;
import org.sonar.db.property.ScrapPropertyDto;
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
import org.sonar.db.qualityprofile.DefaultQProfileMapper;
import org.sonar.db.qualityprofile.QProfileChangeMapper;
import org.sonar.db.qualityprofile.QProfileEditGroupsMapper;
import org.sonar.db.qualityprofile.QProfileEditUsersMapper;
import org.sonar.db.qualityprofile.QualityProfileMapper;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMapper;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryMapper;
import org.sonar.db.schemamigration.SchemaMigrationDto;
import org.sonar.db.schemamigration.SchemaMigrationMapper;
import org.sonar.db.source.FileSourceMapper;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMapper;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipMapper;
import org.sonar.db.user.RoleMapper;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.db.user.UserGroupMapper;
import org.sonar.db.user.UserMapper;
import org.sonar.db.user.UserTokenCount;
import org.sonar.db.user.UserTokenDto;
import org.sonar.db.user.UserTokenMapper;
import org.sonar.db.webhook.WebhookDeliveryMapper;

public class MyBatis implements Startable {

  private final Database database;
  private SqlSessionFactory sessionFactory;

  public MyBatis(Database database) {
    this.database = database;
  }

  @Override
  public void start() {
    LogFactory.useSlf4jLogging();

    MyBatisConfBuilder confBuilder = new MyBatisConfBuilder(database);

    // DTO aliases, keep them sorted alphabetically
    confBuilder.loadAlias("ActiveRule", ActiveRuleDto.class);
    confBuilder.loadAlias("ActiveRuleParam", ActiveRuleParamDto.class);
    confBuilder.loadAlias("CeTaskCharacteristic", CeTaskCharacteristicDto.class);
    confBuilder.loadAlias("Component", ComponentDto.class);
    confBuilder.loadAlias("ComponentLink", ComponentLinkDto.class);
    confBuilder.loadAlias("ComponentWithSnapshot", ComponentDtoWithSnapshotId.class);
    confBuilder.loadAlias("CustomMeasure", CustomMeasureDto.class);
    confBuilder.loadAlias("DuplicationUnit", DuplicationUnitDto.class);
    confBuilder.loadAlias("Event", EventDto.class);
    confBuilder.loadAlias("FilePathWithHash", FilePathWithHashDto.class);
    confBuilder.loadAlias("KeyWithUuid", KeyWithUuidDto.class);
    confBuilder.loadAlias("Group", GroupDto.class);
    confBuilder.loadAlias("GroupMembership", GroupMembershipDto.class);
    confBuilder.loadAlias("GroupPermission", GroupPermissionDto.class);
    confBuilder.loadAlias("IdUuidPair", IdUuidPair.class);
    confBuilder.loadAlias("InternalProperty", InternalPropertyDto.class);
    confBuilder.loadAlias("IssueChange", IssueChangeDto.class);
    confBuilder.loadAlias("KeyLongValue", KeyLongValue.class);
    confBuilder.loadAlias("Issue", IssueDto.class);
    confBuilder.loadAlias("ShortBranchIssue", ShortBranchIssueDto.class);
    confBuilder.loadAlias("Measure", MeasureDto.class);
    confBuilder.loadAlias("NotificationQueue", NotificationQueueDto.class);
    confBuilder.loadAlias("Organization", OrganizationDto.class);
    confBuilder.loadAlias("OrganizationMember", OrganizationMemberDto.class);
    confBuilder.loadAlias("PermissionTemplateCharacteristic", PermissionTemplateCharacteristicDto.class);
    confBuilder.loadAlias("PermissionTemplateGroup", PermissionTemplateGroupDto.class);
    confBuilder.loadAlias("PermissionTemplate", PermissionTemplateDto.class);
    confBuilder.loadAlias("PermissionTemplateUser", PermissionTemplateUserDto.class);
    confBuilder.loadAlias("Plugin", PluginDto.class);
    confBuilder.loadAlias("ProjectQgateAssociation", ProjectQgateAssociationDto.class);
    confBuilder.loadAlias("PurgeableAnalysis", PurgeableAnalysisDto.class);
    confBuilder.loadAlias("QualityGateCondition", QualityGateConditionDto.class);
    confBuilder.loadAlias("QualityGate", QualityGateDto.class);
    confBuilder.loadAlias("RequirementMigration", RequirementMigrationDto.class);
    confBuilder.loadAlias("Resource", ResourceDto.class);
    confBuilder.loadAlias("RuleParam", RuleParamDto.class);
    confBuilder.loadAlias("Rule", RuleDto.class);
    confBuilder.loadAlias("SchemaMigration", SchemaMigrationDto.class);
    confBuilder.loadAlias("ScrapProperty", ScrapPropertyDto.class);
    confBuilder.loadAlias("ScrapAnalysisProperty", ScrapAnalysisPropertyDto.class);
    confBuilder.loadAlias("Snapshot", SnapshotDto.class);
    confBuilder.loadAlias("UserGroup", UserGroupDto.class);
    confBuilder.loadAlias("UserPermission", UserPermissionDto.class);
    confBuilder.loadAlias("UserTokenCount", UserTokenCount.class);
    confBuilder.loadAlias("UserToken", UserTokenDto.class);
    confBuilder.loadAlias("User", UserDto.class);
    confBuilder.loadAlias("UuidWithProjectUuid", UuidWithProjectUuidDto.class);
    confBuilder.loadAlias("ViewsSnapshot", ViewsSnapshotDto.class);

    // keep them sorted alphabetically
    Class<?>[] mappers = {
      ActiveRuleMapper.class,
      AnalysisPropertiesMapper.class,
      AuthorizationMapper.class,
      BranchMapper.class,
      CeActivityMapper.class,
      CeQueueMapper.class,
      CeScannerContextMapper.class,
      CeTaskInputMapper.class,
      CeTaskCharacteristicMapper.class,
      ComponentKeyUpdaterMapper.class,
      ComponentLinkMapper.class,
      ComponentMapper.class,
      LiveMeasureMapper.class,
      CustomMeasureMapper.class,
      DefaultQProfileMapper.class,
      DuplicationMapper.class,
      EsQueueMapper.class,
      EventMapper.class,
      FileSourceMapper.class,
      GroupMapper.class,
      GroupMembershipMapper.class,
      GroupPermissionMapper.class,
      InternalPropertiesMapper.class,
      IsAliveMapper.class,
      IssueChangeMapper.class,
      IssueMapper.class,
      MeasureMapper.class,
      MetricMapper.class,
      NotificationQueueMapper.class,
      OrganizationMapper.class,
      OrganizationMemberMapper.class,
      PermissionTemplateCharacteristicMapper.class,
      PermissionTemplateMapper.class,
      PluginMapper.class,
      ProjectQgateAssociationMapper.class,
      PropertiesMapper.class,
      PurgeMapper.class,
      QProfileChangeMapper.class,
      QProfileEditGroupsMapper.class,
      QProfileEditUsersMapper.class,
      QualityGateConditionMapper.class,
      QualityGateMapper.class,
      QualityProfileMapper.class,
      RoleMapper.class,
      RuleMapper.class,
      RuleRepositoryMapper.class,
      SchemaMigrationMapper.class,
      SnapshotMapper.class,
      UserGroupMapper.class,
      UserMapper.class,
      UserPermissionMapper.class,
      UserTokenMapper.class,
      WebhookDeliveryMapper.class
    };
    confBuilder.loadMappers(mappers);

    sessionFactory = new SqlSessionFactoryBuilder().build(confBuilder.build());
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @VisibleForTesting
  SqlSessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public DbSession openSession(boolean batch) {
    if (batch) {
      SqlSession session = sessionFactory.openSession(ExecutorType.BATCH);
      return new BatchSession(session);
    }
    SqlSession session = sessionFactory.openSession(ExecutorType.REUSE);
    return new DbSessionImpl(session);
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
