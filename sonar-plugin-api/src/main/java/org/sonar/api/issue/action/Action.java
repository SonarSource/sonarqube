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
package org.sonar.api.issue.action;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.Condition;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class Action {

  private final String key;
  private final List<Condition> conditions;
  private final List<Function> functions;

  Action(String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Action key must be set");
    this.key = key;
    this.conditions = newArrayList();
    this.functions = newArrayList();
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

  public Action setFunctions(Function... functions) {
    this.functions.addAll(ImmutableList.copyOf(functions));
    return this;
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
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return key;
  }

}
