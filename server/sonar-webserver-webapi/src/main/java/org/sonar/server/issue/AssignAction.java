/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collection;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.workflow.IsUnResolved;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

@ServerSide
public class AssignAction extends Action {

  public static final String ASSIGN_KEY = "assign";
  public static final String ASSIGNEE_PARAMETER = "assignee";
  private static final String VERIFIED_ASSIGNEE = "verifiedAssignee";

  private final DbClient dbClient;
  private final IssueFieldsSetter issueFieldsSetter;

  public AssignAction(DbClient dbClient, IssueFieldsSetter issueFieldsSetter) {
    super(ASSIGN_KEY);
    this.dbClient = dbClient;
    this.issueFieldsSetter = issueFieldsSetter;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    String assigneeLogin = getAssigneeValue(properties);
    UserDto assignee = isNullOrEmpty(assigneeLogin) ? null : getUser(assigneeLogin);
    properties.put(VERIFIED_ASSIGNEE, assignee);
    return true;
  }

  private static String getAssigneeValue(Map<String, Object> properties) {
    return (String) properties.get(ASSIGNEE_PARAMETER);
  }

  private UserDto getUser(String assigneeKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, assigneeKey), "Unknown user: %s", assigneeKey);
    }
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    checkArgument(properties.containsKey(VERIFIED_ASSIGNEE), "Assignee is missing from the execution parameters");
    UserDto assignee = (UserDto) properties.get(VERIFIED_ASSIGNEE);
    return issueFieldsSetter.assign(context.issue(), assignee, context.issueChangeContext());
  }

  @Override
  public boolean shouldRefreshMeasures() {
    return false;
  }
}
