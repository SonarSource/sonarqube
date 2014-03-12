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

import com.google.common.collect.ImmutableList;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.DashboardDao;
import org.sonar.core.duplication.DuplicationDao;
import org.sonar.core.graph.jdbc.GraphDao;
import org.sonar.core.issue.db.*;
import org.sonar.core.measure.db.MeasureDataDao;
import org.sonar.core.measure.db.MeasureFilterDao;
import org.sonar.core.notification.db.NotificationQueueDao;
import org.sonar.core.permission.PermissionDao;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.RequirementDao;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.user.*;

import java.util.List;

public final class DaoUtils {

  private DaoUtils() {
  }

  @SuppressWarnings("unchecked")
  public static List<Class> getDaoClasses() {
    return ImmutableList.<Class>of(
      ActionPlanDao.class,
      ActionPlanStatsDao.class,
      ActiveDashboardDao.class,
      ActiveRuleDao.class,
      AuthorDao.class,
      AuthorizationDao.class,
      DashboardDao.class,
      DuplicationDao.class,
      GraphDao.class,
      GroupMembershipDao.class,
      IssueDao.class,
      IssueStatsDao.class,
      IssueChangeDao.class,
      IssueFilterDao.class,
      IssueFilterFavouriteDao.class,
      LoadedTemplateDao.class,
      MeasureDataDao.class,
      MeasureFilterDao.class,
      NotificationQueueDao.class,
      PermissionDao.class,
      PermissionTemplateDao.class,
      PropertiesDao.class,
      QualityProfileDao.class,
      PurgeDao.class,
      CharacteristicDao.class,
      RequirementDao.class,
      ResourceIndexerDao.class,
      ResourceDao.class,
      ResourceKeyUpdaterDao.class,
      RoleDao.class,
      RuleDao.class,
      RuleTagDao.class,
      SemaphoreDao.class,
      SnapshotDataDao.class,
      SnapshotSourceDao.class,
      UserDao.class
    );
  }
}
