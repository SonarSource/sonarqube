/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.IdentityHashMap;
import java.util.Map;
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
import org.sonar.db.measure.LiveMeasureDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
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
import org.sonar.db.provisioning.GithubOrganizationGroupDao;
import org.sonar.db.provisioning.GithubPermissionsMappingDao;
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
import org.sonar.db.webhook.WebhookDao;
import org.sonar.db.webhook.WebhookDeliveryDao;

public class DbClient {

  private final Database database;
  private final MyBatis myBatis;
  private final DBSessions dbSessions;

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
  private final UserDao userDao;
  private final UserGroupDao userGroupDao;
  private final UserTokenDao userTokenDao;
  private final GroupMembershipDao groupMembershipDao;
  private final RoleDao roleDao;
  private final GroupPermissionDao groupPermissionDao;
  private final PermissionTemplateDao permissionTemplateDao;
  private final PermissionTemplateCharacteristicDao permissionTemplateCharacteristicDao;
  private final IssueDao issueDao;
  private final RegulatoryReportDao regulatoryReportDao;
  private final IssueChangeDao issueChangeDao;
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
  private final NotificationQueueDao notificationQueueDao;
  private final MetricDao metricDao;
  private final GroupDao groupDao;
  private final ExternalGroupDao externalGroupDao;
  private final RuleDao ruleDao;
  private final RuleRepositoryDao ruleRepositoryDao;
  private final ActiveRuleDao activeRuleDao;
  private final QProfileChangeDao qProfileChangeDao;
  private final UserPermissionDao userPermissionDao;
  private final DefaultQProfileDao defaultQProfileDao;
  private final EsQueueDao esQueueDao;
  private final PluginDao pluginDao;
  private final BranchDao branchDao;
  private final AnalysisPropertiesDao analysisPropertiesDao;
  private final QProfileEditUsersDao qProfileEditUsersDao;
  private final QProfileEditGroupsDao qProfileEditGroupsDao;
  private final LiveMeasureDao liveMeasureDao;
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
  private final GithubPermissionsMappingDao githubPermissionsMappingDao;
  private final RuleChangeDao ruleChangeDao;
  private final ProjectExportDao projectExportDao;
  private final IssueFixedDao issueFixedDao;
  private final TelemetryMetricsSentDao telemetryMetricsSentDao;

  public DbClient(Database database, MyBatis myBatis, DBSessions dbSessions, Dao... daos) {
    this.database = database;
    this.myBatis = myBatis;
    this.dbSessions = dbSessions;

    Map<Class, Dao> map = new IdentityHashMap<>();
    for (Dao dao : daos) {
      map.put(dao.getClass(), dao);
    }
    almSettingDao = getDao(map, AlmSettingDao.class);
    auditDao = getDao(map, AuditDao.class);
    almPatDao = getDao(map, AlmPatDao.class);
    projectAlmSettingDao = getDao(map, ProjectAlmSettingDao.class);
    schemaMigrationDao = getDao(map, SchemaMigrationDao.class);
    authorizationDao = getDao(map, AuthorizationDao.class);
    qualityProfileDao = getDao(map, QualityProfileDao.class);
    qualityProfileExportDao = getDao(map, QualityProfileExportDao.class);
    propertiesDao = getDao(map, PropertiesDao.class);
    internalPropertiesDao = getDao(map, InternalPropertiesDao.class);
    snapshotDao = getDao(map, SnapshotDao.class);
    componentDao = getDao(map, ComponentDao.class);
    componentKeyUpdaterDao = getDao(map, ComponentKeyUpdaterDao.class);
    measureDao = getDao(map, MeasureDao.class);
    userDao = getDao(map, UserDao.class);
    userGroupDao = getDao(map, UserGroupDao.class);
    userTokenDao = getDao(map, UserTokenDao.class);
    groupMembershipDao = getDao(map, GroupMembershipDao.class);
    roleDao = getDao(map, RoleDao.class);
    groupPermissionDao = getDao(map, GroupPermissionDao.class);
    permissionTemplateDao = getDao(map, PermissionTemplateDao.class);
    permissionTemplateCharacteristicDao = getDao(map, PermissionTemplateCharacteristicDao.class);
    issueDao = getDao(map, IssueDao.class);
    issueChangeDao = getDao(map, IssueChangeDao.class);
    ceActivityDao = getDao(map, CeActivityDao.class);
    ceQueueDao = getDao(map, CeQueueDao.class);
    ceTaskInputDao = getDao(map, CeTaskInputDao.class);
    ceTaskCharacteristicsDao = getDao(map, CeTaskCharacteristicDao.class);
    ceScannerContextDao = getDao(map, CeScannerContextDao.class);
    ceTaskMessageDao = getDao(map, CeTaskMessageDao.class);
    fileSourceDao = getDao(map, FileSourceDao.class);
    projectLinkDao = getDao(map, ProjectLinkDao.class);
    eventDao = getDao(map, EventDao.class);
    eventComponentChangeDao = getDao(map, EventComponentChangeDao.class);
    pushEventDao = getDao(map, PushEventDao.class);
    purgeDao = getDao(map, PurgeDao.class);
    qualityGateDao = getDao(map, QualityGateDao.class);
    qualityGateUserPermissionsDao = getDao(map, QualityGateUserPermissionsDao.class);
    gateConditionDao = getDao(map, QualityGateConditionDao.class);
    qualityGateGroupPermissionsDao = getDao(map, QualityGateGroupPermissionsDao.class);
    projectQgateAssociationDao = getDao(map, ProjectQgateAssociationDao.class);
    duplicationDao = getDao(map, DuplicationDao.class);
    regulatoryReportDao = getDao(map, RegulatoryReportDao.class);
    notificationQueueDao = getDao(map, NotificationQueueDao.class);
    metricDao = getDao(map, MetricDao.class);
    groupDao = getDao(map, GroupDao.class);
    githubOrganizationGroupDao = getDao(map, GithubOrganizationGroupDao.class);
    githubPermissionsMappingDao = getDao(map, GithubPermissionsMappingDao.class);
    externalGroupDao = getDao(map, ExternalGroupDao.class);
    ruleDao = getDao(map, RuleDao.class);
    ruleRepositoryDao = getDao(map, RuleRepositoryDao.class);
    activeRuleDao = getDao(map, ActiveRuleDao.class);
    qProfileChangeDao = getDao(map, QProfileChangeDao.class);
    userPermissionDao = getDao(map, UserPermissionDao.class);
    defaultQProfileDao = getDao(map, DefaultQProfileDao.class);
    esQueueDao = getDao(map, EsQueueDao.class);
    pluginDao = getDao(map, PluginDao.class);
    branchDao = getDao(map, BranchDao.class);
    analysisPropertiesDao = getDao(map, AnalysisPropertiesDao.class);
    qProfileEditUsersDao = getDao(map, QProfileEditUsersDao.class);
    qProfileEditGroupsDao = getDao(map, QProfileEditGroupsDao.class);
    liveMeasureDao = getDao(map, LiveMeasureDao.class);
    webhookDao = getDao(map, WebhookDao.class);
    webhookDeliveryDao = getDao(map, WebhookDeliveryDao.class);
    internalComponentPropertiesDao = getDao(map, InternalComponentPropertiesDao.class);
    newCodePeriodDao = getDao(map, NewCodePeriodDao.class);
    projectDao = getDao(map, ProjectDao.class);
    projectBadgeTokenDao = getDao(map, ProjectBadgeTokenDao.class);
    portfolioDao = getDao(map, PortfolioDao.class);
    sessionTokensDao = getDao(map, SessionTokensDao.class);
    samlMessageIdDao = getDao(map, SamlMessageIdDao.class);
    userDismissedMessagesDao = getDao(map, UserDismissedMessagesDao.class);
    applicationProjectsDao = getDao(map, ApplicationProjectsDao.class);
    scannerAnalysisCacheDao = getDao(map, ScannerAnalysisCacheDao.class);
    scimUserDao = getDao(map, ScimUserDao.class);
    scimGroupDao = getDao(map, ScimGroupDao.class);
    entityDao = getDao(map, EntityDao.class);
    reportScheduleDao = getDao(map, ReportScheduleDao.class);
    reportSubscriptionDao = getDao(map, ReportSubscriptionDao.class);
    anticipatedTransitionDao = getDao(map, AnticipatedTransitionDao.class);
    ruleChangeDao = getDao(map, RuleChangeDao.class);
    projectExportDao = getDao(map, ProjectExportDao.class);
    issueFixedDao = getDao(map, IssueFixedDao.class);
    telemetryMetricsSentDao = getDao(map, TelemetryMetricsSentDao.class);
  }

  public DbSession openSession(boolean batch) {
    return dbSessions.openSession(batch);
  }

  public Database getDatabase() {
    return database;
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

  public UserDao userDao() {
    return userDao;
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

  public NotificationQueueDao notificationQueueDao() {
    return notificationQueueDao;
  }

  public MetricDao metricDao() {
    return metricDao;
  }

  public GroupDao groupDao() {
    return groupDao;
  }

  public GithubOrganizationGroupDao githubOrganizationGroupDao() {
    return githubOrganizationGroupDao;
  }

  public GithubPermissionsMappingDao githubPermissionsMappingDao() {
    return githubPermissionsMappingDao;
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

  public LiveMeasureDao liveMeasureDao() {
    return liveMeasureDao;
  }

  protected <K extends Dao> K getDao(Map<Class, Dao> map, Class<K> clazz) {
    return (K) map.get(clazz);
  }

  // should be removed. Still used by some old DAO in sonar-server

  public MyBatis getMyBatis() {
    return myBatis;
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

