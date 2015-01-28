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
package org.sonar.server.issue.notification;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sonar.api.component.Component;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;

import java.util.Date;

public class NewIssuesNotification extends Notification {

  public static final String TYPE = "new-issues";

  public NewIssuesNotification() {
    super(TYPE);
  }

  public NewIssuesNotification setAnalysisDate(Date d) {
    setFieldValue("projectDate", DateUtils.formatDateTime(d));
    return this;
  }

  public NewIssuesNotification setProject(ComponentDto project) {
    setFieldValue("projectName", project.longName());
    setFieldValue("projectKey", project.key());
    setFieldValue("projectUuid", project.uuid());
    return this;
  }

  public NewIssuesNotification setStatistics(Component project, Stats stats) {
    setDefaultMessage(stats.size() + " new issues on " + project.longName() + ".\n");
    setFieldValue("count", String.valueOf(stats.size()));
    for (String severity : Severity.ALL) {
      setFieldValue("count-" + severity, String.valueOf(stats.countIssuesWithSeverity(severity)));
    }
    return this;
  }

  public static class Stats {
    private final Multiset<String> set = HashMultiset.create();

    public void add(Issue issue) {
      set.add(issue.severity());
    }

    public int countIssuesWithSeverity(String severity) {
      return set.count(severity);
    }

    public int size() {
      return set.size();
    }
  }
}
