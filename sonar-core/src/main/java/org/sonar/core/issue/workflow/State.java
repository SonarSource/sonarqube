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
package org.sonar.core.issue.workflow;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;

public class State {
  private final String key;
  private final Transition[] outTransitions;

  public State(String key, Transition[] outTransitions) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "State key must be set");
    Preconditions.checkArgument(StringUtils.isAllUpperCase(key), "State key must be upper-case");
    checkDuplications(outTransitions, key);

    this.key = key;
    this.outTransitions = outTransitions;
  }

  private static void checkDuplications(Transition[] transitions, String stateKey) {
    Set<String> keys = Sets.newHashSet();
    for (Transition transition : transitions) {
      if (keys.contains(transition.key())) {
        throw new IllegalArgumentException("Transition '" + transition.key() +
          "' is declared several times from the originating state '" + stateKey + "'");
      }
      keys.add(transition.key());
    }
  }

  public List<Transition> outManualTransitions(Issue issue) {
    List<Transition> result = Lists.newArrayList();
    for (Transition transition : outTransitions) {
      if (!transition.automatic() && transition.supports(issue)) {
        result.add(transition);
      }
    }
    return result;
  }

  @CheckForNull
  public Transition outAutomaticTransition(Issue issue) {
    Transition result = null;
    for (Transition transition : outTransitions) {
      if (transition.automatic() && transition.supports(issue)) {
        if (result == null) {
          result = transition;
        } else {
          throw new IllegalStateException("Several automatic transitions are available for issue: " + issue);
        }
      }
    }
    return result;
  }

  Transition transition(String transitionKey) {
    for (Transition transition : outTransitions) {
      if (transitionKey.equals(transition.key())) {
        return transition;
      }
    }
    throw new IllegalStateException("Transition from state " + key + " does not exist: " + transitionKey);
  }
}
