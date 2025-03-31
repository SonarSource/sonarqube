/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.workflow.statemachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.permission.ProjectPermission;

import static com.google.common.base.Preconditions.checkArgument;

/**
 *
 * @param <E> The entity the workflow is applied on. Conditions are applied on it.
 * @param <A> The actions that the workflow can trigger during a transition.
 */
public class Transition<E, A> {
  private final String key;
  private final String from;
  private final String to;
  private final List<Predicate<E>> conditions;
  private final List<Consumer<A>> actions;
  private final boolean automatic;
  private final ProjectPermission requiredProjectPermission;

  private Transition(TransitionBuilder<E, A> builder) {
    key = builder.key;
    from = builder.from;
    to = builder.to;
    conditions = List.copyOf(builder.conditions);
    actions = List.copyOf(builder.actions);
    automatic = builder.automatic;
    requiredProjectPermission = builder.requiredProjectPermission;
  }

  public String key() {
    return key;
  }

  String from() {
    return from;
  }

  public String to() {
    return to;
  }

  List<Predicate<E>> conditions() {
    return conditions;
  }

  public List<Consumer<A>> actions() {
    return actions;
  }

  public boolean automatic() {
    return automatic;
  }

  public boolean supports(E entity) {
    for (Predicate<E> condition : conditions) {
      if (!condition.test(entity)) {
        return false;
      }
    }
    return true;
  }

  @CheckForNull
  public ProjectPermission requiredProjectPermission() {
    return requiredProjectPermission;
  }

  @Override
  public String toString() {
    return String.format("%s->%s->%s", from, key, to);
  }

  public static <E, A> Transition<E, A> create(String key, String from, String to) {
    return Transition.<E, A>builder(key).from(from).to(to).build();
  }

  public static <E, A> TransitionBuilder<E, A> builder(String key) {
    return new TransitionBuilder<>(key);
  }

  public static class TransitionBuilder<E, A> {
    private final String key;
    private String from;
    private String to;
    private final List<Predicate<E>> conditions = new ArrayList<>();
    private final List<Consumer<A>> actions = new ArrayList<>();
    private boolean automatic = false;
    private ProjectPermission requiredProjectPermission;

    private TransitionBuilder(String key) {
      this.key = key;
    }

    public TransitionBuilder<E, A> from(String from) {
      this.from = from;
      return this;
    }

    public TransitionBuilder<E, A> to(String to) {
      this.to = to;
      return this;
    }

    @SafeVarargs
    public final TransitionBuilder<E, A> conditions(Predicate<E>... conditions) {
      this.conditions.addAll(Arrays.asList(conditions));
      return this;
    }

    @SafeVarargs
    public final TransitionBuilder<E, A> actions(Consumer<A>... actions) {
      this.actions.addAll(Arrays.asList(actions));
      return this;
    }

    public TransitionBuilder<E, A> automatic() {
      this.automatic = true;
      return this;
    }

    public TransitionBuilder<E, A> requiredProjectPermission(ProjectPermission requiredProjectPermission) {
      this.requiredProjectPermission = requiredProjectPermission;
      return this;
    }

    public Transition<E, A> build() {
      checkArgument(StringUtils.isNotEmpty(key), "Transition key must be set");
      checkArgument(StringUtils.isAllLowerCase(key), "Transition key must be lower-case");
      checkArgument(StringUtils.isNotEmpty(from), "Originating status must be set");
      checkArgument(StringUtils.isNotEmpty(to), "Destination status must be set");
      return new Transition<>(this);
    }
  }
}
