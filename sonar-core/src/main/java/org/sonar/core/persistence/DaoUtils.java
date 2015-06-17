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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.DashboardDao;
import org.sonar.core.duplication.DuplicationDao;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanStatsDao;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueFilterDao;
import org.sonar.core.issue.db.IssueFilterFavouriteDao;
import org.sonar.core.notification.db.NotificationQueueDao;
import org.sonar.core.permission.PermissionDao;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceIndexerDao;
import org.sonar.core.resource.ResourceKeyUpdaterDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.user.AuthorDao;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.core.user.GroupMembershipDao;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;

import static com.google.common.collect.Lists.newArrayList;

public final class DaoUtils {

  private static final int PARTITION_SIZE_FOR_ORACLE = 1000;

  private DaoUtils() {
    // only static stuff
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
