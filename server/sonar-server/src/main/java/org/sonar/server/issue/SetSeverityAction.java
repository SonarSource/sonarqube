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
package org.sonar.server.issue;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

@ServerSide
public class SetSeverityAction extends Action {

  public static final String SET_SEVERITY_KEY = "set_severity";
  public static final String SEVERITY_PARAMETER = "severity";

  private final IssueFieldsSetter issueUpdater;
  private final UserSession userSession;

  public SetSeverityAction(IssueFieldsSetter issueUpdater, UserSession userSession) {
    super(SET_SEVERITY_KEY);
    this.issueUpdater = issueUpdater;
    this.userSession = userSession;
    super.setConditions(new IsUnResolved(), issue -> isCurrentUserIssueAdmin(issue.projectUuid()));
  }

  private boolean isCurrentUserIssueAdmin(String projectUuid) {
    return userSession.hasComponentUuidPermission(UserRole.ISSUE_ADMIN, projectUuid);
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    severity(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    return issueUpdater.setManualSeverity(context.issue(), severity(properties), context.issueChangeContext());
  }

  private static String severity(Map<String, Object> properties) {
    String param = (String) properties.get(SEVERITY_PARAMETER);
    if (Strings.isNullOrEmpty(param)) {
      throw new IllegalArgumentException("Missing parameter : 'severity'");
    }
    return param;
  }
}
