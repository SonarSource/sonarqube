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
package org.sonar.server.issue.actionplan;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import org.sonar.api.issue.ActionPlan;

/**
 * Sort action plans by chronological deadlines. Plans without deadline are
 * located after plans with deadline.
 */
public class ActionPlanDeadlineComparator implements Comparator<ActionPlan>, Serializable {

  @Override
  public int compare(ActionPlan a1, ActionPlan a2) {
    Date d1 = a1.deadLine();
    Date d2 = a2.deadLine();
    if (d1 != null && d2 != null) {
      return d1.compareTo(d2);
    }
    if (d1 != null) {
      return -1;
    }
    return 1;
  }
}
