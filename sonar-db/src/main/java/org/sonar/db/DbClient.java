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

import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.activity.ActivityDao;
import org.sonar.db.ce.CeActivityDao;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentLinkDao;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceIndexDao;
import org.sonar.db.component.ResourceKeyUpdaterDao;
import org.sonar.db.component.SnapshotDao;
import org.sonar.db.dashboard.ActiveDashboardDao;
import org.sonar.db.dashboard.DashboardDao;
import org.sonar.db.dashboard.WidgetDao;
import org.sonar.db.dashboard.WidgetPropertyDao;
import org.sonar.db.duplication.DuplicationDao;
import org.sonar.db.event.EventDao;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.issue.IssueFilterFavouriteDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureFilterDao;
import org.sonar.db.measure.MeasureFilterFavouriteDao;
import org.sonar.db.measure.custom.CustomMeasureDao;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.qualitygate.ProjectQgateAssociationDao;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.user.AuthorDao;
import org.sonar.db.user.AuthorizationDao;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserGroupDao;
import org.sonar.db.user.UserTokenDao;

public class DbClient {

  private final Database database;
  private final MyBatis myBatis;
  private final QualityProfileDao qualityProfileDao;
  private final LoadedTemplateDao loadedTemplateDao;
  private final PropertiesDao propertiesDao;
  private final SnapshotDao snapshotDao;
  private final ComponentDao componentDao;
  private final ResourceDao resourceDao;
  private final ResourceKeyUpdaterDao resourceKeyUpdaterDao;
  private final MeasureDao measureDao;
  private final MeasureFilterDao measureFilterDao;
  private final MeasureFilterFavouriteDao measureFilterFavouriteDao;
  private final ActivityDao activityDao;
  private final AuthorizationDao authorizationDao;
  private final UserDao userDao;
  private final UserGroupDao userGroupDao;
  private final UserTokenDao userTokenDao;
  private final GroupMembershipDao groupMembershipDao;
  private final RoleDao roleDao;
  private final PermissionDao permissionDao;
  private final PermissionTemplateDao permissionTemplateDao;
  private final IssueDao issueDao;
  private final IssueFilterDao issueFilterDao;
  private final IssueFilterFavouriteDao issueFilterFavouriteDao;
  private final IssueChangeDao issueChangeDao;
  private final CeQueueDao ceQueueDao;
  private final CeActivityDao ceActivityDao;
  private final DashboardDao dashboardDao;
  private final ActiveDashboardDao activeDashboardDao;
  private final WidgetDao widgetDao;
  private final WidgetPropertyDao widgetPropertyDao;
  private final FileSourceDao fileSourceDao;
  private final AuthorDao authorDao;
  private final ResourceIndexDao componentIndexDao;
  private final ComponentLinkDao componentLinkDao;
  private final EventDao eventDao;
  private final PurgeDao purgeDao;
  private final QualityGateDao qualityGateDao;
  private final QualityGateConditionDao gateConditionDao;
  private final ProjectQgateAssociationDao projectQgateAssociationDao;
  private final DuplicationDao duplicationDao;
  private final NotificationQueueDao notificationQueueDao;
  private final CustomMeasureDao customMeasureDao;
  private final MetricDao metricDao;
  private final GroupDao groupDao;
  private final RuleDao ruleDao;
  private final ActiveRuleDao activeRuleDao;

  public DbClient(Database database, MyBatis myBatis, Dao... daos) {
    this.database = database;
    this.myBatis = myBatis;

    Map<Class, Dao> map = new IdentityHashMap<>();
    for (Dao dao : daos) {
      map.put(dao.getClass(), dao);
    }
    qualityProfileDao = getDao(map, QualityProfileDao.class);
    loadedTemplateDao = getDao(map, LoadedTemplateDao.class);
    propertiesDao = getDao(map, PropertiesDao.class);
    snapshotDao = getDao(map, SnapshotDao.class);
    componentDao = getDao(map, ComponentDao.class);
    resourceDao = getDao(map, ResourceDao.class);
    resourceKeyUpdaterDao = getDao(map, ResourceKeyUpdaterDao.class);
    measureDao = getDao(map, MeasureDao.class);
    measureFilterDao = getDao(map, MeasureFilterDao.class);
    measureFilterFavouriteDao = getDao(map, MeasureFilterFavouriteDao.class);
    activityDao = getDao(map, ActivityDao.class);
    authorizationDao = getDao(map, AuthorizationDao.class);
    userDao = getDao(map, UserDao.class);
    userGroupDao = getDao(map, UserGroupDao.class);
    userTokenDao = getDao(map, UserTokenDao.class);
    groupMembershipDao = getDao(map, GroupMembershipDao.class);
    roleDao = getDao(map, RoleDao.class);
    permissionDao = getDao(map, PermissionDao.class);
    permissionTemplateDao = getDao(map, PermissionTemplateDao.class);
    issueDao = getDao(map, IssueDao.class);
    issueFilterDao = getDao(map, IssueFilterDao.class);
    issueFilterFavouriteDao = getDao(map, IssueFilterFavouriteDao.class);
    issueChangeDao = getDao(map, IssueChangeDao.class);
    ceQueueDao = getDao(map, CeQueueDao.class);
    ceActivityDao = getDao(map, CeActivityDao.class);
    dashboardDao = getDao(map, DashboardDao.class);
    activeDashboardDao = getDao(map, ActiveDashboardDao.class);
    widgetDao = getDao(map, WidgetDao.class);
    widgetPropertyDao = getDao(map, WidgetPropertyDao.class);
    fileSourceDao = getDao(map, FileSourceDao.class);
    authorDao = getDao(map, AuthorDao.class);
    componentIndexDao = getDao(map, ResourceIndexDao.class);
    componentLinkDao = getDao(map, ComponentLinkDao.class);
    eventDao = getDao(map, EventDao.class);
    purgeDao = getDao(map, PurgeDao.class);
    qualityGateDao = getDao(map, QualityGateDao.class);
    gateConditionDao = getDao(map, QualityGateConditionDao.class);
    projectQgateAssociationDao = getDao(map, ProjectQgateAssociationDao.class);
    duplicationDao = getDao(map, DuplicationDao.class);
    notificationQueueDao = getDao(map, NotificationQueueDao.class);
    customMeasureDao = getDao(map, CustomMeasureDao.class);
    metricDao = getDao(map, MetricDao.class);
    groupDao = getDao(map, GroupDao.class);
    ruleDao = getDao(map, RuleDao.class);
    activeRuleDao = getDao(map, ActiveRuleDao.class);
    doOnLoad(map);
  }

  // should be removed, but till used by sonar-server
  protected void doOnLoad(Map<Class, Dao> daoByClass) {

  }

  public DbSession openSession(boolean batch) {
    return myBatis.openSession(batch);
  }

  public void closeSession(@Nullable DbSession session) {
    MyBatis.closeQuietly(session);
  }

  public Database getDatabase() {
    return database;
  }

  public IssueDao issueDao() {
    return issueDao;
  }

  public IssueFilterDao issueFilterDao() {
    return issueFilterDao;
  }

  public IssueFilterFavouriteDao issueFilterFavouriteDao() {
    return issueFilterFavouriteDao;
  }

  public IssueChangeDao issueChangeDao() {
    return issueChangeDao;
  }

  public QualityProfileDao qualityProfileDao() {
    return qualityProfileDao;
  }

  public LoadedTemplateDao loadedTemplateDao() {
    return loadedTemplateDao;
  }

  public PropertiesDao propertiesDao() {
    return propertiesDao;
  }

  public SnapshotDao snapshotDao() {
    return snapshotDao;
  }

  public ComponentDao componentDao() {
    return componentDao;
  }

  public ResourceDao resourceDao() {
    return resourceDao;
  }

  public ResourceKeyUpdaterDao resourceKeyUpdaterDao() {
    return resourceKeyUpdaterDao;
  }

  public MeasureDao measureDao() {
    return measureDao;
  }

  public MeasureFilterDao measureFilterDao() {
    return measureFilterDao;
  }

  public MeasureFilterFavouriteDao measureFilterFavouriteDao() {
    return measureFilterFavouriteDao;
  }

  public ActivityDao activityDao() {
    return activityDao;
  }

  public AuthorizationDao authorizationDao() {
    return authorizationDao;
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

  public PermissionDao permissionDao() {
    return permissionDao;
  }

  public PermissionTemplateDao permissionTemplateDao() {
    return permissionTemplateDao;
  }

  public CeQueueDao ceQueueDao() {
    return ceQueueDao;
  }

  public CeActivityDao ceActivityDao() {
    return ceActivityDao;
  }

  public DashboardDao dashboardDao() {
    return dashboardDao;
  }

  public ActiveDashboardDao activeDashboardDao() {
    return activeDashboardDao;
  }

  public WidgetDao widgetDao() {
    return widgetDao;
  }

  public WidgetPropertyDao widgetPropertyDao() {
    return widgetPropertyDao;
  }

  public FileSourceDao fileSourceDao() {
    return fileSourceDao;
  }

  public AuthorDao authorDao() {
    return authorDao;
  }

  public ResourceIndexDao componentIndexDao() {
    return componentIndexDao;
  }

  public ComponentLinkDao componentLinkDao() {
    return componentLinkDao;
  }

  public EventDao eventDao() {
    return eventDao;
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

  public ProjectQgateAssociationDao projectQgateAssociationDao() {
    return projectQgateAssociationDao;
  }

  public DuplicationDao duplicationDao() {
    return duplicationDao;
  }

  public NotificationQueueDao notificationQueueDao() {
    return notificationQueueDao;
  }

  public CustomMeasureDao customMeasureDao() {
    return customMeasureDao;
  }

  public MetricDao metricDao() {
    return metricDao;
  }

  public GroupDao groupDao() {
    return groupDao;
  }

  public RuleDao ruleDao() {
    return ruleDao;
  }

  public ActiveRuleDao activeRuleDao() {
    return activeRuleDao;
  }

  protected <K extends Dao> K getDao(Map<Class, Dao> map, Class<K> clazz) {
    return (K) map.get(clazz);
  }

  // should be removed. Still used by some old DAO in sonar-server
  public MyBatis getMyBatis() {
    return myBatis;
  }
}
