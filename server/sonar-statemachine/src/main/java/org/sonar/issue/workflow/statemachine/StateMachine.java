/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.issue.workflow.statemachine;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 *
 * @param <E> The entity the workflow is applied on. Conditions are applied on it.
 * @param <A> The actions that the workflow can trigger during a transition.
 */
public class StateMachine<E, A> {

  private final List<String> keys;
  private final Map<String, State<E, A>> byKey;

  private StateMachine(Builder<E, A> builder) {
    this.keys = List.copyOf(builder.states);
    Map<String, State<E, A>> mapBuilder = new HashMap<>();
    for (String stateKey : builder.states) {
      List<Transition<E, A>> outTransitions = builder.outTransitions.get(stateKey);
      State<E, A> state = new State<>(stateKey, outTransitions);
      mapBuilder.put(stateKey, state);
    }
    byKey = Map.copyOf(mapBuilder);
  }

  @CheckForNull
  public State<E, A> state(String stateKey) {
    return byKey.get(stateKey);
  }

  public List<String> stateKeys() {
    return keys;
  }

  public static <E, A> Builder<E, A> builder() {
    return new Builder<>();
  }

  public static class Builder<E, A> {
    private final Set<String> states = new LinkedHashSet<>();
    // transitions per originating state
    private final ListMultimap<String, Transition<E, A>> outTransitions = ArrayListMultimap.create();

    private Builder() {
    }

    public Builder<E, A> states(String... keys) {
      states.addAll(Arrays.asList(keys));
      return this;
    }

    public Builder<E, A> transition(Transition<E, A> transition) {
      Preconditions.checkArgument(states.contains(transition.from()), "Originating state does not exist: " + transition.from());
      Preconditions.checkArgument(states.contains(transition.to()), "Destination state does not exist: " + transition.to());
      outTransitions.put(transition.from(), transition);
      return this;
    }

    public StateMachine<E, A> build() {
      Preconditions.checkArgument(!states.isEmpty(), "At least one state is required");
      return new StateMachine<>(this);
    }
  }
}
