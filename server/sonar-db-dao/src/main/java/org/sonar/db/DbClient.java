/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import org.sonar.db.adminalert.AdminAlertStatusDao;
import org.sonar.db.agent.AgentJobDao;
import org.sonar.db.alm.pat.AlmPatDao;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.audit.AuditDao;
import org.sonar.db.ce.CeActivityDao;
import org.sonar.db.ce.CeQueueDao;
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
import org.sonar.db.entity.EntityDao;
import org.sonar.db.es.EsQueueDao;
import org.sonar.db.event.EventComponentChangeDao;
import org.sonar.db.event.EventDao;
import org.sonar.db.issue.AnticipatedTransitionDao;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueFixedDao;
import org.sonar.db.jira.dao.AtlassianAuthenticationDetailsDao;
import org.sonar.db.jira.dao.JiraOrganizationBindingDao;
import org.sonar.db.jira.dao.JiraOrganizationBindingPendingDao;
import org.sonar.db.jira.dao.JiraPermissionDao;
import org.sonar.db.jira.dao.JiraProjectBindingDao;
import org.sonar.db.jira.dao.JiraSelectedWorkTypeDao;
import org.sonar.db.jira.dao.JiraWorkItemDao;
import org.sonar.db.jira.dao.XsrfTokenDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.ProjectMeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.migrationlog.MigrationLogDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.db.notification.NotificationGroupSubscriptionsDao;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.permission.GroupPermissionDao;
import org.sonar.db.permission.UserPermissionDao;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDao;
import org.sonar.db.permission.template.PermissionTemplateDao;
import org.sonar.db.plugin.PluginDao;
import org.sonar.db.portfolio.PortfolioDao;
import org.sonar.db.project.ProjectBadgeTokenDao;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectExportDao;
import org.sonar.db.property.InternalComponentPropertiesDao;
import org.sonar.db.property.InternalPropertiesDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.provisioning.DevOpsPermissionsMappingDao;
import org.sonar.db.provisioning.GithubOrganizationGroupDao;
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
import org.sonar.db.report.ReportScheduleDao;
import org.sonar.db.report.ReportSubscriptionDao;
import org.sonar.db.rule.RuleChangeDao;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleRepositoryDao;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;
import org.sonar.db.schemamigration.SchemaMigrationDao;
import org.sonar.db.scim.ScimGroupDao;
import org.sonar.db.scim.ScimUserDao;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.telemetry.TelemetryMetricsSentDao;
import org.sonar.db.user.ExternalGroupDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.SamlMessageIdDao;
import org.sonar.db.user.SessionTokensDao;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDismissedMessagesDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserTokenDao;
import org.sonar.db.user.ai.UserAiToolUsageDao;
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDeliveryDao;

public class DbClient extends AbstractDbClient {
  private final SchemaMigrationDao schemaMigrationDao;
  private final AuthorizationDao authorizationDao;
  private final QualityProfileDao qualityProfileDao;
  private final QualityProfileExportDao qualityProfileExportDao;
  private final PropertiesDao propertiesDao;
  private final AlmSettingDao almSettingDao;
  private final AlmPatDao almPatDao;
  private final AuditDao auditDao;
  private final ProjectAlmSettingDao projectAlmSettingDao;
  private final InternalComponentPropertiesDao internalComponentPropertiesDao;
  private final InternalPropertiesDao internalPropertiesDao;
  private final SnapshotDao snapshotDao;
  private final ComponentDao componentDao;
  private final ComponentKeyUpdaterDao componentKeyUpdaterDao;
  private final MeasureDao measureDao;
  private final ProjectMeasureDao projectMeasureDao;
  private final UserDao userDao;
  private final UserGroupDao userGroupDao;
  private final UserTokenDao userTokenDao;
  private final UserAiToolUsageDao userAiToolUsageDao;
  private final GroupMembershipDao groupMembershipDao;
  private final RoleDao roleDao;
  private final GroupPermissionDao groupPermissionDao;
  private final PermissionTemplateDao permissionTemplateDao;
  private final PermissionTemplateCharacteristicDao permissionTemplateCharacteristicDao;
  private final IssueDao issueDao;
  private final RegulatoryReportDao regulatoryReportDao;
  private final IssueChangeDao issueChangeDao;
  private final JiraProjectBindingDao jiraProjectBindingDao;
  private final JiraOrganizationBindingDao jiraOrganizationBindingDao;
  private final JiraWorkItemDao jiraWorkItemDao;
  private final JiraOrganizationBindingPendingDao jiraOrganizationBindingPendingDao;
  private final AtlassianAuthenticationDetailsDao atlassianAuthenticationDetailsDao;
  private final XsrfTokenDao xsrfTokenDao;
  private final JiraSelectedWorkTypeDao jiraSelectedWorkTypeDao;
  private final JiraPermissionDao jiraPermissionDao;
  private final CeActivityDao ceActivityDao;
  private final CeQueueDao ceQueueDao;
  private final CeTaskInputDao ceTaskInputDao;
  private final CeTaskCharacteristicDao ceTaskCharacteristicsDao;
  private final CeScannerContextDao ceScannerContextDao;
  private final CeTaskMessageDao ceTaskMessageDao;
  private final FileSourceDao fileSourceDao;
  private final ProjectLinkDao projectLinkDao;
  private final EventDao eventDao;
  private final EventComponentChangeDao eventComponentChangeDao;
  private final PushEventDao pushEventDao;
  private final PurgeDao purgeDao;
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao gateConditionDao;
  private final QualityGateGroupPermissionsDao qualityGateGroupPermissionsDao;
  private final QualityGateUserPermissionsDao qualityGateUserPermissionsDao;
  private final ProjectQgateAssociationDao projectQgateAssociationDao;
  private final DuplicationDao duplicationDao;
  private final NotificationGroupSubscriptionsDao notificationGroupSubscriptionsDao;
  private final NotificationQueueDao notificationQueueDao;
  private final MetricDao metricDao;
  private final MigrationLogDao migrationLogDao;
  private final GroupDao groupDao;
  private final ExternalGroupDao externalGroupDao;
  private final RuleDao ruleDao;
  private final RuleRepositoryDao ruleRepositoryDao;
  private final ActiveRuleDao activeRuleDao;
  private final AdminAlertStatusDao adminAlertStatusDao;
  private final AgentJobDao agentJobDao;
  private final QProfileChangeDao qProfileChangeDao;
  private final UserPermissionDao userPermissionDao;
  private final DefaultQProfileDao defaultQProfileDao;
  private final EsQueueDao esQueueDao;
  private final PluginDao pluginDao;
  private final BranchDao branchDao;
  private final AnalysisPropertiesDao analysisPropertiesDao;
  private final QProfileEditUsersDao qProfileEditUsersDao;
  private final QProfileEditGroupsDao qProfileEditGroupsDao;
  private final WebhookDao webhookDao;
  private final WebhookDeliveryDao webhookDeliveryDao;
  private final NewCodePeriodDao newCodePeriodDao;
  private final ProjectDao projectDao;
  private final PortfolioDao portfolioDao;
  private final SessionTokensDao sessionTokensDao;
  private final SamlMessageIdDao samlMessageIdDao;
  private final UserDismissedMessagesDao userDismissedMessagesDao;
  private final ApplicationProjectsDao applicationProjectsDao;
  private final ProjectBadgeTokenDao projectBadgeTokenDao;
  private final ScannerAnalysisCacheDao scannerAnalysisCacheDao;
  private final ScimUserDao scimUserDao;
  private final ScimGroupDao scimGroupDao;
  private final EntityDao entityDao;
  private final AnticipatedTransitionDao anticipatedTransitionDao;
  private final ReportScheduleDao reportScheduleDao;
  private final ReportSubscriptionDao reportSubscriptionDao;
  private final GithubOrganizationGroupDao githubOrganizationGroupDao;
  private final DevOpsPermissionsMappingDao devopsPermissionsMappingDao;
  private final RuleChangeDao ruleChangeDao;
  private final ProjectExportDao projectExportDao;
  private final IssueFixedDao issueFixedDao;
  private final TelemetryMetricsSentDao telemetryMetricsSentDao;

  public DbClient(MyBatis myBatis, DBSessions dbSessions, Dao... daos) {
    super(myBatis, dbSessions, daos);

    almSettingDao = getDaoOrThrow(AlmSettingDao.class);
    auditDao = getDaoOrThrow(AuditDao.class);
    almPatDao = getDaoOrThrow(AlmPatDao.class);
    projectAlmSettingDao = getDaoOrThrow(ProjectAlmSettingDao.class);
    schemaMigrationDao = getDaoOrThrow(SchemaMigrationDao.class);
    authorizationDao = getDaoOrThrow(AuthorizationDao.class);
    qualityProfileDao = getDaoOrThrow(QualityProfileDao.class);
    qualityProfileExportDao = getDaoOrThrow(QualityProfileExportDao.class);
    propertiesDao = getDaoOrThrow(PropertiesDao.class);
    internalPropertiesDao = getDaoOrThrow(InternalPropertiesDao.class);
    snapshotDao = getDaoOrThrow(SnapshotDao.class);
    componentDao = getDaoOrThrow(ComponentDao.class);
    componentKeyUpdaterDao = getDaoOrThrow(ComponentKeyUpdaterDao.class);
    measureDao = getDaoOrThrow(MeasureDao.class);
    projectMeasureDao = getDaoOrThrow(ProjectMeasureDao.class);
    userDao = getDaoOrThrow(UserDao.class);
    userGroupDao = getDaoOrThrow(UserGroupDao.class);
    userTokenDao = getDaoOrThrow(UserTokenDao.class);
    userAiToolUsageDao = getDaoOrThrow(UserAiToolUsageDao.class);
    groupMembershipDao = getDaoOrThrow(GroupMembershipDao.class);
    roleDao = getDaoOrThrow(RoleDao.class);
    groupPermissionDao = getDaoOrThrow(GroupPermissionDao.class);
    permissionTemplateDao = getDaoOrThrow(PermissionTemplateDao.class);
    permissionTemplateCharacteristicDao = getDaoOrThrow(PermissionTemplateCharacteristicDao.class);
    issueDao = getDaoOrThrow(IssueDao.class);
    issueChangeDao = getDaoOrThrow(IssueChangeDao.class);
    jiraProjectBindingDao = getDaoOrThrow(JiraProjectBindingDao.class);
    jiraOrganizationBindingDao = getDaoOrThrow(JiraOrganizationBindingDao.class);
    jiraWorkItemDao = getDaoOrThrow(JiraWorkItemDao.class);
    jiraOrganizationBindingPendingDao = getDaoOrThrow(JiraOrganizationBindingPendingDao.class);
    atlassianAuthenticationDetailsDao = getDaoOrThrow(AtlassianAuthenticationDetailsDao.class);
    xsrfTokenDao = getDaoOrThrow(XsrfTokenDao.class);
    jiraSelectedWorkTypeDao = getDaoOrThrow(JiraSelectedWorkTypeDao.class);
    jiraPermissionDao = getDaoOrThrow(JiraPermissionDao.class);
    ceActivityDao = getDaoOrThrow(CeActivityDao.class);
    ceQueueDao = getDaoOrThrow(CeQueueDao.class);
    ceTaskInputDao = getDaoOrThrow(CeTaskInputDao.class);
    ceTaskCharacteristicsDao = getDaoOrThrow(CeTaskCharacteristicDao.class);
    ceScannerContextDao = getDaoOrThrow(CeScannerContextDao.class);
    ceTaskMessageDao = getDaoOrThrow(CeTaskMessageDao.class);
    fileSourceDao = getDaoOrThrow(FileSourceDao.class);
    projectLinkDao = getDaoOrThrow(ProjectLinkDao.class);
    eventDao = getDaoOrThrow(EventDao.class);
    eventComponentChangeDao = getDaoOrThrow(EventComponentChangeDao.class);
    pushEventDao = getDaoOrThrow(PushEventDao.class);
    purgeDao = getDaoOrThrow(PurgeDao.class);
    qualityGateDao = getDaoOrThrow(QualityGateDao.class);
    qualityGateUserPermissionsDao = getDaoOrThrow(QualityGateUserPermissionsDao.class);
    gateConditionDao = getDaoOrThrow(QualityGateConditionDao.class);
    qualityGateGroupPermissionsDao = getDaoOrThrow(QualityGateGroupPermissionsDao.class);
    projectQgateAssociationDao = getDaoOrThrow(ProjectQgateAssociationDao.class);
    duplicationDao = getDaoOrThrow(DuplicationDao.class);
    regulatoryReportDao = getDaoOrThrow(RegulatoryReportDao.class);
    notificationGroupSubscriptionsDao = getDaoOrThrow(NotificationGroupSubscriptionsDao.class);
    notificationQueueDao = getDaoOrThrow(NotificationQueueDao.class);
    metricDao = getDaoOrThrow(MetricDao.class);
    migrationLogDao = getDaoOrThrow(MigrationLogDao.class);
    groupDao = getDaoOrThrow(GroupDao.class);
    githubOrganizationGroupDao = getDaoOrThrow(GithubOrganizationGroupDao.class);
    devopsPermissionsMappingDao = getDaoOrThrow(DevOpsPermissionsMappingDao.class);
    externalGroupDao = getDaoOrThrow(ExternalGroupDao.class);
    ruleDao = getDaoOrThrow(RuleDao.class);
    ruleRepositoryDao = getDaoOrThrow(RuleRepositoryDao.class);
    activeRuleDao = getDaoOrThrow(ActiveRuleDao.class);
    adminAlertStatusDao = getDaoOrThrow(AdminAlertStatusDao.class);
    agentJobDao = getDaoOrThrow(AgentJobDao.class);
    qProfileChangeDao = getDaoOrThrow(QProfileChangeDao.class);
    userPermissionDao = getDaoOrThrow(UserPermissionDao.class);
    defaultQProfileDao = getDaoOrThrow(DefaultQProfileDao.class);
    esQueueDao = getDaoOrThrow(EsQueueDao.class);
    pluginDao = getDaoOrThrow(PluginDao.class);
    branchDao = getDaoOrThrow(BranchDao.class);
    analysisPropertiesDao = getDaoOrThrow(AnalysisPropertiesDao.class);
    qProfileEditUsersDao = getDaoOrThrow(QProfileEditUsersDao.class);
    qProfileEditGroupsDao = getDaoOrThrow(QProfileEditGroupsDao.class);
    webhookDao = getDaoOrThrow(WebhookDao.class);
    webhookDeliveryDao = getDaoOrThrow(WebhookDeliveryDao.class);
    internalComponentPropertiesDao = getDaoOrThrow(InternalComponentPropertiesDao.class);
    newCodePeriodDao = getDaoOrThrow(NewCodePeriodDao.class);
    projectDao = getDaoOrThrow(ProjectDao.class);
    projectBadgeTokenDao = getDaoOrThrow(ProjectBadgeTokenDao.class);
    portfolioDao = getDaoOrThrow(PortfolioDao.class);
    sessionTokensDao = getDaoOrThrow(SessionTokensDao.class);
    samlMessageIdDao = getDaoOrThrow(SamlMessageIdDao.class);
    userDismissedMessagesDao = getDaoOrThrow(UserDismissedMessagesDao.class);
    applicationProjectsDao = getDaoOrThrow(ApplicationProjectsDao.class);
    scannerAnalysisCacheDao = getDaoOrThrow(ScannerAnalysisCacheDao.class);
    scimUserDao = getDaoOrThrow(ScimUserDao.class);
    scimGroupDao = getDaoOrThrow(ScimGroupDao.class);
    entityDao = getDaoOrThrow(EntityDao.class);
    reportScheduleDao = getDaoOrThrow(ReportScheduleDao.class);
    reportSubscriptionDao = getDaoOrThrow(ReportSubscriptionDao.class);
    anticipatedTransitionDao = getDaoOrThrow(AnticipatedTransitionDao.class);
    ruleChangeDao = getDaoOrThrow(RuleChangeDao.class);
    projectExportDao = getDaoOrThrow(ProjectExportDao.class);
    issueFixedDao = getDaoOrThrow(IssueFixedDao.class);
    telemetryMetricsSentDao = getDaoOrThrow(TelemetryMetricsSentDao.class);
  }

  public AlmSettingDao almSettingDao() {
    return almSettingDao;
  }

  public AlmPatDao almPatDao() {
    return almPatDao;
  }

  public ApplicationProjectsDao applicationProjectsDao() {
    return applicationProjectsDao;
  }

  public AuditDao auditDao() {
    return auditDao;
  }

  public ProjectAlmSettingDao projectAlmSettingDao() {
    return projectAlmSettingDao;
  }

  public SchemaMigrationDao schemaMigrationDao() {
    return schemaMigrationDao;
  }

  public AuthorizationDao authorizationDao() {
    return authorizationDao;
  }

  public IssueDao issueDao() {
    return issueDao;
  }

  public RegulatoryReportDao regulatoryReportDao() {
    return regulatoryReportDao;
  }

  public IssueChangeDao issueChangeDao() {
    return issueChangeDao;
  }

  public IssueFixedDao issueFixedDao() {
    return issueFixedDao;
  }

  public JiraProjectBindingDao jiraProjectBindingDao() {
    return jiraProjectBindingDao;
  }

  public JiraOrganizationBindingDao jiraOrganizationBindingDao() {
    return jiraOrganizationBindingDao;
  }

  public JiraWorkItemDao jiraWorkItemDao() {
    return jiraWorkItemDao;
  }

  public JiraOrganizationBindingPendingDao jiraOrganizationBindingPendingDao() {
    return jiraOrganizationBindingPendingDao;
  }

  public AtlassianAuthenticationDetailsDao atlassianAuthenticationDetailsDao() {
    return atlassianAuthenticationDetailsDao;
  }

  public XsrfTokenDao xsrfTokenDao() {
    return xsrfTokenDao;
  }

  public JiraSelectedWorkTypeDao jiraSelectedWorkTypeDao() {
    return jiraSelectedWorkTypeDao;
  }

  public JiraPermissionDao jiraPermissionDao() {
    return jiraPermissionDao;
  }

  public TelemetryMetricsSentDao telemetryMetricsSentDao() {
    return telemetryMetricsSentDao;
  }

  public QualityProfileDao qualityProfileDao() {
    return qualityProfileDao;
  }

  public QualityProfileExportDao qualityProfileExportDao() {
    return qualityProfileExportDao;
  }

  public PropertiesDao propertiesDao() {
    return propertiesDao;
  }

  public InternalPropertiesDao internalPropertiesDao() {
    return internalPropertiesDao;
  }

  public SnapshotDao snapshotDao() {
    return snapshotDao;
  }

  public AnalysisPropertiesDao analysisPropertiesDao() {
    return analysisPropertiesDao;
  }

  public ComponentDao componentDao() {
    return componentDao;
  }

  public ProjectDao projectDao() {
    return projectDao;
  }

  public PortfolioDao portfolioDao() {
    return portfolioDao;
  }

  public ComponentKeyUpdaterDao componentKeyUpdaterDao() {
    return componentKeyUpdaterDao;
  }

  public MeasureDao measureDao() {
    return measureDao;
  }

  public ProjectMeasureDao projectMeasureDao() {
    return projectMeasureDao;
  }

  public UserDao userDao() {
    return userDao;
  }

  public UserAiToolUsageDao userAiToolUsageDao() {
    return userAiToolUsageDao;
  }

  public UserGroupDao userGroupDao() {
    return userGroupDao;
  }

  public UserTokenDao userTokenDao() {
    return userTokenDao;
  }

  public GroupMembershipDao groupMembershipDao() {
    return groupMembershipDao;
  }

  public RoleDao roleDao() {
    return roleDao;
  }

  public GroupPermissionDao groupPermissionDao() {
    return groupPermissionDao;
  }

  public PermissionTemplateDao permissionTemplateDao() {
    return permissionTemplateDao;
  }

  public PermissionTemplateCharacteristicDao permissionTemplateCharacteristicDao() {
    return permissionTemplateCharacteristicDao;
  }

  public CeActivityDao ceActivityDao() {
    return ceActivityDao;
  }

  public CeQueueDao ceQueueDao() {
    return ceQueueDao;
  }

  public CeTaskInputDao ceTaskInputDao() {
    return ceTaskInputDao;
  }

  public CeTaskCharacteristicDao ceTaskCharacteristicsDao() {
    return ceTaskCharacteristicsDao;
  }

  public CeScannerContextDao ceScannerContextDao() {
    return ceScannerContextDao;
  }

  public CeTaskMessageDao ceTaskMessageDao() {
    return ceTaskMessageDao;
  }

  public FileSourceDao fileSourceDao() {
    return fileSourceDao;
  }

  public ProjectLinkDao projectLinkDao() {
    return projectLinkDao;
  }

  public EventDao eventDao() {
    return eventDao;
  }

  public EventComponentChangeDao eventComponentChangeDao() {
    return eventComponentChangeDao;
  }

  public PushEventDao pushEventDao() {
    return pushEventDao;
  }

  public PurgeDao purgeDao() {
    return purgeDao;
  }

  public QualityGateDao qualityGateDao() {
    return qualityGateDao;
  }

  public QualityGateConditionDao gateConditionDao() {
    return gateConditionDao;
  }

  public QualityGateUserPermissionsDao qualityGateUserPermissionDao() {
    return qualityGateUserPermissionsDao;
  }

  public QualityGateGroupPermissionsDao qualityGateGroupPermissionsDao() {
    return qualityGateGroupPermissionsDao;
  }

  public ProjectQgateAssociationDao projectQgateAssociationDao() {
    return projectQgateAssociationDao;
  }

  public DuplicationDao duplicationDao() {
    return duplicationDao;
  }

  public NotificationGroupSubscriptionsDao notificationGroupSubscriptionsDao() {
    return notificationGroupSubscriptionsDao;
  }

  public NotificationQueueDao notificationQueueDao() {
    return notificationQueueDao;
  }

  public MetricDao metricDao() {
    return metricDao;
  }

  public MigrationLogDao migrationLogDao() {
    return migrationLogDao;
  }

  public GroupDao groupDao() {
    return groupDao;
  }

  public GithubOrganizationGroupDao githubOrganizationGroupDao() {
    return githubOrganizationGroupDao;
  }

  public DevOpsPermissionsMappingDao githubPermissionsMappingDao() {
    return devopsPermissionsMappingDao;
  }

  public ExternalGroupDao externalGroupDao() {
    return externalGroupDao;
  }

  public RuleDao ruleDao() {
    return ruleDao;
  }

  public RuleRepositoryDao ruleRepositoryDao() {
    return ruleRepositoryDao;
  }

  public ActiveRuleDao activeRuleDao() {
    return activeRuleDao;
  }

  public AdminAlertStatusDao adminAlertStatusDao() {
    return adminAlertStatusDao;
  }

  public AgentJobDao agentJobDao() {
    return agentJobDao;
  }

  public QProfileChangeDao qProfileChangeDao() {
    return qProfileChangeDao;
  }

  public UserPermissionDao userPermissionDao() {
    return userPermissionDao;
  }

  public DefaultQProfileDao defaultQProfileDao() {
    return defaultQProfileDao;
  }

  public EsQueueDao esQueueDao() {
    return esQueueDao;
  }

  public PluginDao pluginDao() {
    return pluginDao;
  }

  public BranchDao branchDao() {
    return branchDao;
  }

  public QProfileEditUsersDao qProfileEditUsersDao() {
    return qProfileEditUsersDao;
  }

  public QProfileEditGroupsDao qProfileEditGroupsDao() {
    return qProfileEditGroupsDao;
  }

  public WebhookDao webhookDao() {
    return webhookDao;
  }

  public WebhookDeliveryDao webhookDeliveryDao() {
    return webhookDeliveryDao;
  }

  public InternalComponentPropertiesDao internalComponentPropertiesDao() {
    return internalComponentPropertiesDao;
  }

  public NewCodePeriodDao newCodePeriodDao() {
    return newCodePeriodDao;
  }

  public SessionTokensDao sessionTokensDao() {
    return sessionTokensDao;
  }

  public SamlMessageIdDao samlMessageIdDao() {
    return samlMessageIdDao;
  }

  public UserDismissedMessagesDao userDismissedMessagesDao() {
    return userDismissedMessagesDao;
  }

  public ProjectBadgeTokenDao projectBadgeTokenDao() {
    return projectBadgeTokenDao;
  }

  public ScannerAnalysisCacheDao scannerAnalysisCacheDao() {
    return scannerAnalysisCacheDao;
  }

  public ScimUserDao scimUserDao() {
    return scimUserDao;
  }

  public ScimGroupDao scimGroupDao() {
    return scimGroupDao;
  }

  public EntityDao entityDao() {
    return entityDao;
  }

  public ReportScheduleDao reportScheduleDao() {
    return reportScheduleDao;
  }

  public ReportSubscriptionDao reportSubscriptionDao() {
    return reportSubscriptionDao;
  }

  public AnticipatedTransitionDao anticipatedTransitionDao() {
    return anticipatedTransitionDao;
  }

  public RuleChangeDao ruleChangeDao() {
    return ruleChangeDao;
  }

  public ProjectExportDao projectExportDao() {
    return projectExportDao;
  }
}
