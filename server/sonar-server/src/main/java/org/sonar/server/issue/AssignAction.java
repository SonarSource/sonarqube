/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.server.ServerSide;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

@ServerSide
public class AssignAction extends Action {

  public static final String ASSIGN_KEY = "assign";
  public static final String ASSIGNEE_PARAMETER = "assignee";
  public static final String VERIFIED_ASSIGNEE = "verifiedAssignee";

  private final UserFinder userFinder;
  private final IssueFieldsSetter issueUpdater;

  public AssignAction(UserFinder userFinder, IssueFieldsSetter issueUpdater) {
    super(ASSIGN_KEY);
    this.userFinder = userFinder;
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    String assignee = assigneeValue(properties);
    if (!Strings.isNullOrEmpty(assignee)) {
      User user = selectUser(assignee);
      if (user == null) {
        throw new IllegalArgumentException("Unknown user: " + assignee);
      }
      properties.put(VERIFIED_ASSIGNEE, user);
    } else {
      properties.put(VERIFIED_ASSIGNEE, null);
    }
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    if (!properties.containsKey(VERIFIED_ASSIGNEE)) {
      throw new IllegalArgumentException("Assignee is missing from the execution parameters");
    }
    User assignee = (User) properties.get(VERIFIED_ASSIGNEE);
    return issueUpdater.assign(context.issue(), assignee, context.issueChangeContext());
  }

  private String assigneeValue(Map<String, Object> properties) {
    return (String) properties.get(ASSIGNEE_PARAMETER);
  }

  private User selectUser(String assigneeKey) {
    return userFinder.findByLogin(assigneeKey);
  }
}
