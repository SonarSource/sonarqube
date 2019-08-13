/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.workflow.Condition;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.7
 */
@ServerSide
public abstract class Action {

  private final String key;
  private final List<Condition> conditions;

  public Action(String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Action key must be set");
    this.key = key;
    this.conditions = newArrayList();
  }

  public String key() {
    return key;
  }

  public Action setConditions(Condition... conditions) {
    this.conditions.addAll(ImmutableList.copyOf(conditions));
    return this;
  }

  public List<Condition> conditions() {
    return conditions;
  }

  public boolean supports(DefaultIssue issue) {
    for (Condition condition : conditions) {
      if (!condition.matches(issue)) {
        return false;
      }
    }
    return true;
  }

  public abstract boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession);

  public abstract boolean execute(Map<String, Object> properties, Context context);

  public abstract boolean shouldRefreshMeasures();

  public interface Context {
    DefaultIssue issue();

    IssueChangeContext issueChangeContext();

    ComponentDto project();
  }

}
