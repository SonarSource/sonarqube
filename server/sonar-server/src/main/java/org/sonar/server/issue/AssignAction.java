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

import java.util.Collection;
import java.util.Map;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.sonar.server.ws.WsUtils.checkFound;

@ServerSide
public class AssignAction extends Action {

  public static final String ASSIGN_KEY = "assign";
  public static final String ASSIGNEE_PARAMETER = "assignee";
  public static final String VERIFIED_ASSIGNEE = "verifiedAssignee";

  private final DbClient dbClient;
  private final IssueFieldsSetter issueUpdater;

  public AssignAction(DbClient dbClient, IssueFieldsSetter issueUpdater) {
    super(ASSIGN_KEY);
    this.dbClient = dbClient;
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    String assignee = getAssigneeValue(properties);
    properties.put(VERIFIED_ASSIGNEE, isNullOrEmpty(assignee) ? null : getUser(assignee));
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    checkArgument(properties.containsKey(VERIFIED_ASSIGNEE), "Assignee is missing from the execution parameters");
    UserDto assignee = (UserDto) properties.get(VERIFIED_ASSIGNEE);
    return issueUpdater.assign(context.issue(), assignee, context.issueChangeContext());
  }

  private static String getAssigneeValue(Map<String, Object> properties) {
    return (String) properties.get(ASSIGNEE_PARAMETER);
  }

  private UserDto getUser(String assigneeKey) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return checkFound(dbClient.userDao().selectByLogin(dbSession, assigneeKey), "Unknown user: %s", assigneeKey);
    }
  }
}
