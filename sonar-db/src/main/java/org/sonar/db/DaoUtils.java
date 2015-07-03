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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.db.component.ResourceDao;
import org.sonar.db.component.ResourceIndexerDao;
import org.sonar.db.component.ResourceKeyUpdaterDao;
import org.sonar.db.dashboard.ActiveDashboardDao;
import org.sonar.db.dashboard.DashboardDao;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.duplication.DuplicationDao;
import org.sonar.db.issue.ActionPlanDao;
import org.sonar.db.issue.ActionPlanStatsDao;
import org.sonar.db.issue.IssueChangeDao;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueFilterDao;
import org.sonar.db.issue.IssueFilterFavouriteDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.permission.PermissionDao;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.semaphore.SemaphoreDao;
import org.sonar.db.user.AuthorDao;
import org.sonar.db.user.AuthorizationDao;
import org.sonar.db.user.GroupMembershipDao;
import org.sonar.db.user.RoleDao;
import org.sonar.db.user.UserDao;

import static com.google.common.collect.Lists.newArrayList;

public final class DaoUtils {

  private static final int PARTITION_SIZE_FOR_ORACLE = 1000;

  private DaoUtils() {
    // only static stuff
  }

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
      GroupMembershipDao.class,
      IssueDao.class,
      IssueChangeDao.class,
      IssueFilterDao.class,
      IssueFilterFavouriteDao.class,
      LoadedTemplateDao.class,
      NotificationQueueDao.class,
      PermissionDao.class,
      PermissionTemplateDao.class,
      PropertiesDao.class,
      QualityGateConditionDao.class,
      QualityProfileDao.class,
      PurgeDao.class,
      CharacteristicDao.class,
      ResourceIndexerDao.class,
      ResourceDao.class,
      ResourceKeyUpdaterDao.class,
      RoleDao.class,
      RuleDao.class,
      SemaphoreDao.class,
      UserDao.class
      );
  }

  /**
   * Partition by 1000 elements a list of input and execute a function on each part.
   *
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <OUTPUT, INPUT> List<OUTPUT> executeLargeInputs(Collection<INPUT> input, Function<List<INPUT>, List<OUTPUT>> function) {
    if (input.isEmpty()) {
      return Collections.emptyList();
    }
    List<OUTPUT> results = newArrayList();
    List<List<INPUT>> partitionList = Lists.partition(newArrayList(input), PARTITION_SIZE_FOR_ORACLE);
    for (List<INPUT> partition : partitionList) {
      List<OUTPUT> subResults = function.apply(partition);
      results.addAll(subResults);
    }
    return results;
  }

  /**
   * Partition by 1000 elements a list of input and execute a function on each part.
   * The function has not output (ex: delete operation)
   *
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <INPUT> void executeLargeInputsWithoutOutput(Collection<INPUT> input, Function<List<INPUT>, Void> function) {
    if (input.isEmpty()) {
      return;
    }

    List<List<INPUT>> partitions = Lists.partition(newArrayList(input), PARTITION_SIZE_FOR_ORACLE);
    for (List<INPUT> partition : partitions) {
      function.apply(partition);
    }
  }

  public static String repeatCondition(String sql, int count, String separator) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(sql);
      if (i < count - 1) {
        sb.append(" ").append(separator).append(" ");
      }
    }
    return sb.toString();
  }
}
