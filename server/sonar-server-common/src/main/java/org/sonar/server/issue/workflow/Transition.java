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
package org.sonar.server.issue.workflow;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;

import static com.google.common.base.Preconditions.checkArgument;

public class Transition {
  private final String key;
  private final String from;
  private final String to;
  private final Condition[] conditions;
  private final Function[] functions;
  private final boolean automatic;
  private String requiredProjectPermission;

  private Transition(TransitionBuilder builder) {
    key = builder.key;
    from = builder.from;
    to = builder.to;
    conditions = builder.conditions.toArray(new Condition[builder.conditions.size()]);
    functions = builder.functions.toArray(new Function[builder.functions.size()]);
    automatic = builder.automatic;
    requiredProjectPermission = builder.requiredProjectPermission;
  }

  public String key() {
    return key;
  }

  String from() {
    return from;
  }

  String to() {
    return to;
  }

  Condition[] conditions() {
    return conditions;
  }

  Function[] functions() {
    return functions;
  }

  boolean automatic() {
    return automatic;
  }

  public boolean supports(Issue issue) {
    for (Condition condition : conditions) {
      if (!condition.matches(issue)) {
        return false;
      }
    }
    return true;
  }

  @CheckForNull
  public String requiredProjectPermission() {
    return requiredProjectPermission;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Transition that = (Transition) o;
    if (!from.equals(that.from)) {
      return false;
    }
    if (!key.equals(that.key)) {
      return false;
    }
    return to.equals(that.to);
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + from.hashCode();
    result = 31 * result + to.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s->%s->%s", from, key, to);
  }

  public static Transition create(String key, String from, String to) {
    return builder(key).from(from).to(to).build();
  }

  public static TransitionBuilder builder(String key) {
    return new TransitionBuilder(key);
  }

  public static class TransitionBuilder {
    private final String key;
    private String from;
    private String to;
    private List<Condition> conditions = Lists.newArrayList();
    private List<Function> functions = Lists.newArrayList();
    private boolean automatic = false;
    private String requiredProjectPermission;

    private TransitionBuilder(String key) {
      this.key = key;
    }

    public TransitionBuilder from(String from) {
      this.from = from;
      return this;
    }

    public TransitionBuilder to(String to) {
      this.to = to;
      return this;
    }

    public TransitionBuilder conditions(Condition... c) {
      this.conditions.addAll(Arrays.asList(c));
      return this;
    }

    public TransitionBuilder functions(Function... f) {
      this.functions.addAll(Arrays.asList(f));
      return this;
    }

    public TransitionBuilder automatic() {
      this.automatic = true;
      return this;
    }

    public TransitionBuilder requiredProjectPermission(String requiredProjectPermission) {
      this.requiredProjectPermission = requiredProjectPermission;
      return this;
    }

    public Transition build() {
      checkArgument(!Strings.isNullOrEmpty(key), "Transition key must be set");
      checkArgument(StringUtils.isAllLowerCase(key), "Transition key must be lower-case");
      checkArgument(!Strings.isNullOrEmpty(from), "Originating status must be set");
      checkArgument(!Strings.isNullOrEmpty(to), "Destination status must be set");
      return new Transition(this);
    }
  }
}
