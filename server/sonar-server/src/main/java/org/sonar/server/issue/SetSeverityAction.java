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

package org.sonar.server.issue;

import com.google.common.base.Strings;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.Condition;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Map;

@ServerSide
public class SetSeverityAction extends Action {

  public static final String KEY = "set_severity";

  private final IssueUpdater issueUpdater;

  public SetSeverityAction(IssueUpdater issueUpdater) {
    super(KEY);
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved(), new Condition() {
      @Override
      public boolean matches(Issue issue) {
        return isCurrentUserIssueAdmin(issue.projectKey());
      }
    });
  }

  private boolean isCurrentUserIssueAdmin(String projectKey) {
    return UserSession.get().hasProjectPermission(UserRole.ISSUE_ADMIN, projectKey);
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
    severity(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    return issueUpdater.setManualSeverity((DefaultIssue) context.issue(), severity(properties), context.issueChangeContext());
  }

  private String severity(Map<String, Object> properties) {
    String param = (String) properties.get("severity");
    if (Strings.isNullOrEmpty(param)) {
      throw new IllegalArgumentException("Missing parameter : 'severity'");
    }
    return param;
  }
}
