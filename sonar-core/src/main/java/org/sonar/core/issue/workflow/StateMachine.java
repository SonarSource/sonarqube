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
package org.sonar.core.issue.workflow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateMachine {

  private Map<String, State> states = Maps.newHashMap();

  private StateMachine(Builder builder) {
    for (String stateKey : builder.states) {
      List<Transition> outTransitions = builder.outTransitions.get(stateKey);
      State state = new State(stateKey, outTransitions.toArray(new Transition[outTransitions.size()]));
      states.put(stateKey, state);
    }
  }

  @CheckForNull
  public State state(String stateKey) {
    return states.get(stateKey);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Set<String> states = Sets.newTreeSet();
    // transitions per originating state
    private final ListMultimap<String, Transition> outTransitions = ArrayListMultimap.create();

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
