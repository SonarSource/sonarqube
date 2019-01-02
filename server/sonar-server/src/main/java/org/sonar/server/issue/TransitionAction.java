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

import java.util.Collection;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@ServerSide
public class TransitionAction extends Action {

  public static final String DO_TRANSITION_KEY = "do_transition";
  public static final String TRANSITION_PARAMETER = "transition";

  private final TransitionService transitionService;

  public TransitionAction(TransitionService transitionService) {
    super(DO_TRANSITION_KEY);
    this.transitionService = transitionService;
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<DefaultIssue> issues, UserSession userSession) {
    transition(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    DefaultIssue issue = context.issue();
    String transition = transition(properties);
    return canExecuteTransition(issue, transition) && transitionService.doTransition(context.issue(), context.issueChangeContext(), transition(properties));
  }

  @Override
  public boolean shouldRefreshMeasures() {
    return true;
  }

  private boolean canExecuteTransition(DefaultIssue issue, String transitionKey) {
    return transitionService.listTransitions(issue)
      .stream()
      .map(Transition::key)
      .collect(MoreCollectors.toSet())
      .contains(transitionKey);
  }

  private static String transition(Map<String, Object> properties) {
    String param = (String) properties.get(TRANSITION_PARAMETER);
    checkArgument(!isNullOrEmpty(param), "Missing parameter : 'transition'");
    return param;
  }

}
