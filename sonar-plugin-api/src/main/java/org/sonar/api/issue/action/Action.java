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
package org.sonar.api.issue.action;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.Condition;

import java.util.Arrays;
import java.util.List;

public class Action {

  private final String key;
  private final List<Condition> conditions;
  private final List<Function> functions;

  private Action(ActionBuilder builder) {
    key = builder.key;
    conditions = builder.conditions;
    functions = builder.functions;
  }

  public String key() {
    return key;
  }

  public List<Condition> conditions() {
    return conditions;
  }

  public List<Function> functions() {
    return functions;
  }

  public boolean supports(Issue issue) {
    for (Condition condition : conditions) {
      if (!condition.matches(issue)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Action that = (Action) o;
    if (!key.equals(that.key)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return key;
  }

  public static Action create(String key) {
    return builder(key).build();
  }

  public static ActionBuilder builder(String key) {
    return new ActionBuilder(key);
  }

  public static class ActionBuilder {
    private final String key;
    private List<Condition> conditions = Lists.newArrayList();
    private List<Function> functions = Lists.newArrayList();

    private ActionBuilder(String key) {
      this.key = key;
    }

    public ActionBuilder conditions(Condition... c) {
      this.conditions.addAll(Arrays.asList(c));
      return this;
    }

    public ActionBuilder functions(Function... f) {
      this.functions.addAll(Arrays.asList(f));
      return this;
    }

    public Action build() {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Action key must be set");
      return new Action(this);
    }
  }
}
