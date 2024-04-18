/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.List;
import org.sonar.core.platform.Module;
import org.sonar.db.alm.pat.AlmPatDao;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.audit.AuditDao;
import org.sonar.db.ce.CeActivityDao;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.db.ce.CsQueueDao;
import org.sonar.db.ce.CeScannerContextDao;
import org.sonar.db.ce.CeTaskCharacteristicDao;
import org.sonar.db.ce.CeTaskInputDao;
import org.sonar.db.ce.CeTaskMessageDao;
import org.sonar.db.component.AnalysisPropertiesDao;
import org.sonar.db.component.ApplicationProjectsDao;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentKeyUpdaterDao;
import org.sonar.db.component.ProjectLinkDao;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.duplication.DuplicationDao;
import org.sonar.db.es.EsQueueDao;
import org.sonar.db.event.EventComponentChangeDao;
import org.sonar.db.event.EventDao;
import org.sonar.db.ideusage.IdeUsageDao;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.mapping.ProjectMappingsDao;
import org.sonar.db.measure.LiveMeasureDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.organization.OrganizationDao;
import org.sonar.db.organization.OrganizationMemberDao;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.permission.GroupPermissionDao;
import org.sonar.db.permission.UserPermissionDao;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDao;
import org.sonar.db.permission.template.PermissionTemplateDao;
import org.sonar.db.plugin.PluginDao;
import org.sonar.db.portfolio.PortfolioDao;
import org.sonar.db.project.ProjectBadgeTokenDao;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.property.InternalComponentPropertiesDao;
import org.sonar.db.property.InternalPropertiesDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.pushevent.PushEventDao;
import org.sonar.db.qualitygate.ProjectQgateAssociationDao;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateGroupPermissionsDao;
import org.sonar.db.qualitygate.QualityGateUserPermissionsDao;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.DefaultQProfileDao;
import org.sonar.db.qualityprofile.QProfileChangeDao;
import org.sonar.db.qualityprofile.QProfileEditGroupsDao;
import org.sonar.db.qualityprofile.QProfileEditUsersDao;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileExportDao;
import org.sonar.db.report.RegulatoryReportDao;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleRepositoryDao;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;
import org.sonar.db.schemamigration.SchemaMigrationDao;
import org.sonar.db.scim.ScimUserDao;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.SamlMessageIdDao;
import org.sonar.db.user.SessionTokensDao;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDismissedMessagesDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserTokenDao;
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDeliveryDao;

public class DaoModule extends Module {
  private static final List<Class<? extends Dao>> classes = List.of(
    // =====================================================================
    // for readability and easier merge, keep list ordered alphabetically
    // =====================================================================
    ActiveRuleDao.class,
    AnalysisPropertiesDao.class,
    AuthorizationDao.class,
    ApplicationProjectsDao.class,
    AuditDao.class,
    BranchDao.class,
    CeActivityDao.class,
    CeQueueDao.class,
    CsQueueDao.class,
    CeScannerContextDao.class,
    CeTaskCharacteristicDao.class,
    CeTaskInputDao.class,
    CeTaskMessageDao.class,
    ComponentDao.class,
    ComponentKeyUpdaterDao.class,
    DefaultQProfileDao.class,
    DuplicationDao.class,
    EsQueueDao.class,
    EventDao.class,
    EventComponentChangeDao.class,
    FileSourceDao.class,
    GroupDao.class,
    GroupMembershipDao.class,
    GroupPermissionDao.class,
    AlmSettingDao.class,
    AlmPatDao.class,
    ProjectAlmSettingDao.class,
    InternalComponentPropertiesDao.class,
    InternalPropertiesDao.class,
    IssueChangeDao.class,
    IssueDao.class,
    LiveMeasureDao.class,
    MeasureDao.class,
    MetricDao.class,
    NewCodePeriodDao.class,
    NotificationQueueDao.class,
    OrganizationDao.class,
    OrganizationMemberDao.class,
    PermissionTemplateCharacteristicDao.class,
    PermissionTemplateDao.class,
    PluginDao.class,
    ProjectDao.class,
    ProjectBadgeTokenDao.class,
    PortfolioDao.class,
    ProjectLinkDao.class,
    ProjectMappingsDao.class,
    ProjectQgateAssociationDao.class,
    PropertiesDao.class,
    IdeUsageDao.class,
    PurgeDao.class,
    PushEventDao.class,
    QProfileChangeDao.class,
    QProfileEditGroupsDao.class,
    QProfileEditUsersDao.class,
    QualityGateConditionDao.class,
    QualityGateDao.class,
    QualityGateGroupPermissionsDao.class,
    QualityGateUserPermissionsDao.class,
    QualityProfileDao.class,
    QualityProfileExportDao.class,
    RegulatoryReportDao.class,
    RoleDao.class,
    RuleDao.class,
    RuleRepositoryDao.class,
    SamlMessageIdDao.class,
    ScannerAnalysisCacheDao.class,
    SchemaMigrationDao.class,
    ScimUserDao.class,
    SnapshotDao.class,
    SessionTokensDao.class,
    UserDao.class,
    UserDismissedMessagesDao.class,
    UserGroupDao.class,
    UserPermissionDao.class,
    UserTokenDao.class,
    WebhookDao.class,
    WebhookDeliveryDao.class);

  @Override
  protected void configureModule() {
    add(classes.toArray());
  }

  public static List<Class<? extends Dao>> classes() {
    return classes;
  }
}
