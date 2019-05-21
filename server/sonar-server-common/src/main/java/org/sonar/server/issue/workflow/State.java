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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.Issue;

public class State {
  private final String key;
  private final Transition[] outTransitions;

  public State(String key, Transition[] outTransitions) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "State key must be set");
    checkDuplications(outTransitions, key);

    this.key = key;
    this.outTransitions = outTransitions;
  }

  private static void checkDuplications(Transition[] transitions, String stateKey) {
    Set<String> keys = new HashSet<>();

    Arrays.stream(transitions)
      .filter(transition -> !keys.add(transition.key()))
      .findAny()
      .ifPresent(transition -> {
        throw new IllegalArgumentException("Transition '" + transition.key() +
          "' is declared several times from the originating state '" + stateKey + "'");
      });
  }

  public List<Transition> outManualTransitions(Issue issue) {
    return Arrays.stream(outTransitions)
      .filter(transition -> !transition.automatic())
      .filter(transition -> transition.supports(issue))
      .collect(Collectors.toList());
  }

  @CheckForNull
  public Transition outAutomaticTransition(Issue issue) {
    final Transition[] result = new Transition[1];
    Set<String> keys = new HashSet<>();

    Arrays.stream(outTransitions)
      .filter(Transition::automatic)
      .filter(transition -> transition.supports(issue))
      .peek(transition -> result[0] = transition)
      .filter(transition -> !keys.add(transition.key()))
      .findAny()
      .ifPresent(transition -> {
        throw new IllegalArgumentException("Several automatic transitions are available for issue: " + issue);
      });

    return result[0];
  }

  Transition transition(String transitionKey) {
    return Arrays.stream(outTransitions)
      .filter(transition -> transitionKey.equals(transition.key()))
      .findAny()
      .orElseThrow(() -> new IllegalArgumentException("Transition from state " + key + " does not exist: " + transitionKey));
  }
}
