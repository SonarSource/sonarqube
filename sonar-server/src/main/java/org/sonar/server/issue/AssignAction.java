/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.user.UserFinder;
import org.sonar.server.user.UserSession;

import java.util.Map;


public class AssignAction extends Action implements ServerComponent {

  public static final String ASSIGN_ACTION_KEY = "assign";

  private final UserFinder userFinder;

  public AssignAction(UserFinder userFinder) {
    super(ASSIGN_ACTION_KEY);
    this.userFinder = userFinder;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, UserSession userSession){
    String assignee = assignee(properties);
    if (assignee != null && userFinder.findByLogin(assignee) == null) {
      throw new IllegalArgumentException("Unknown user: " + assignee);
    }
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    context.issueUpdater().assign((DefaultIssue) context.issue(), assignee(properties), context.issueChangeContext());
    return true;
  }

  private String assignee(Map<String, Object> properties){
    return (String) properties.get("assignee");
  }
}