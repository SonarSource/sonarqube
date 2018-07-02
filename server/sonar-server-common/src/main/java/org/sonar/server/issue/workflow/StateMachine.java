/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;

public class StateMachine {

  private final List<String> keys;
  private final Map<String, State> byKey;

  private StateMachine(Builder builder) {
    this.keys = ImmutableList.copyOf(builder.states);
    ImmutableMap.Builder<String, State> mapBuilder = ImmutableMap.builder();
    for (String stateKey : builder.states) {
      List<Transition> outTransitions = builder.outTransitions.get(stateKey);
      State state = new State(stateKey, outTransitions.toArray(new Transition[outTransitions.size()]));
      mapBuilder.put(stateKey, state);
    }
    byKey = mapBuilder.build();
  }

  @CheckForNull
  public State state(String stateKey) {
    return byKey.get(stateKey);
  }

  public List<String> stateKeys() {
    return keys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Set<String> states = new LinkedHashSet<>();
    // transitions per originating state
    private final ListMultimap<String, Transition> outTransitions = ArrayListMultimap.create();

    private Builder() {
    }

    public Builder states(String... keys) {
      states.addAll(Arrays.asList(keys));
      return this;
    }

    public Builder transition(Transition transition) {
      Preconditions.checkArgument(states.contains(transition.from()), "Originating state does not exist: " + transition.from());
      Preconditions.checkArgument(states.contains(transition.to()), "Destination state does not exist: " + transition.to());
      outTransitions.put(transition.from(), transition);
      return this;
    }

    public StateMachine build() {
      Preconditions.checkArgument(!states.isEmpty(), "At least one state is required");
      return new StateMachine(this);
    }
  }
}
