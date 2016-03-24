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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.core.platform.Module;
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

public class DaoModule extends Module {
  private static final List<Class<? extends Dao>> classes = ImmutableList.<Class<? extends Dao>>builder().add(
    ActiveDashboardDao.class,
    ActivityDao.class,
    AuthorDao.class,
    AuthorizationDao.class,
    CeActivityDao.class,
    CeQueueDao.class,
    ComponentDao.class,
    ComponentLinkDao.class,
    CustomMeasureDao.class,
    DashboardDao.class,
    DuplicationDao.class,
    EventDao.class,
    FileSourceDao.class,
    GroupDao.class,
    GroupMembershipDao.class,
    IssueDao.class,
    IssueChangeDao.class,
    IssueFilterDao.class,
    IssueFilterFavouriteDao.class,
    LoadedTemplateDao.class,
    MeasureDao.class,
    MeasureFilterDao.class,
    MeasureFilterFavouriteDao.class,
    MetricDao.class,
    NotificationQueueDao.class,
    PermissionDao.class,
    PermissionTemplateDao.class,
    PropertiesDao.class,
    QualityGateDao.class,
    QualityGateConditionDao.class,
    ProjectQgateAssociationDao.class,
    QualityProfileDao.class,
    PurgeDao.class,
    RuleDao.class,
    ActiveRuleDao.class,
    ResourceIndexDao.class,
    ResourceDao.class,
    ResourceKeyUpdaterDao.class,
    RoleDao.class,
    SnapshotDao.class,
    UserDao.class,
    UserGroupDao.class,
    UserTokenDao.class,
    WidgetDao.class,
    WidgetPropertyDao.class).build();

  @Override
  protected void configureModule() {
    add(classes.toArray());
  }

  public static List<Class<? extends Dao>> classes() {
    return classes;
  }
}
