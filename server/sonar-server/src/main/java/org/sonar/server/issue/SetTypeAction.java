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

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SetTypeAction extends Action {

  private static final String SET_TYPE_KEY = "set_type";
  private static final String TYPE_PARAMETER = "type";

  private final IssueUpdater issueUpdater;

  public SetTypeAction(IssueUpdater issueUpdater) {
    super(SET_TYPE_KEY);
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
    newValue(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    String type = newValue(properties);
    return issueUpdater.setType((DefaultIssue) context.issue(), RuleType.valueOf(type), context.issueChangeContext());
  }

  private static String newValue(Map<String, Object> properties) {
    String type = (String) properties.get(TYPE_PARAMETER);
    Preconditions.checkArgument(!isNullOrEmpty(type), "Missing parameter: '%s'", TYPE_PARAMETER);
    Preconditions.checkArgument(RuleType.names().contains(type), "Unknown type: %s", type);
    return type;
  }
}
