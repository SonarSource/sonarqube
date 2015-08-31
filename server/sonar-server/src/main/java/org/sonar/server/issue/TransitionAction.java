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

package org.sonar.server.issue;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.user.UserSession;

@ServerSide
public class TransitionAction extends Action {

  public static final String DO_TRANSITION_KEY = "do_transition";

  private final IssueWorkflow workflow;
  private final UserSession userSession;

  public TransitionAction(IssueWorkflow workflow, UserSession userSession) {
    super(DO_TRANSITION_KEY);
    this.workflow = workflow;
    this.userSession = userSession;
  }

  @Override
  public boolean verify(Map<String, Object> properties, Collection<Issue> issues, UserSession userSession) {
    transition(properties);
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    DefaultIssue issue = (DefaultIssue) context.issue();
    String transition = transition(properties);
    if (canExecuteTransition(issue, transition)) {
      return workflow.doTransition((DefaultIssue) context.issue(), transition(properties), context.issueChangeContext());
    }
    return false;
  }

  private boolean canExecuteTransition(Issue issue, final String transition) {
    final DefaultIssue defaultIssue = (DefaultIssue) issue;
    return Iterables.find(workflow.outTransitions(issue), new Predicate<Transition>() {
      @Override
      public boolean apply(Transition input) {
        return input.key().equals(transition) &&
          (StringUtils.isBlank(input.requiredProjectPermission()) ||
          userSession.hasProjectPermission(input.requiredProjectPermission(), defaultIssue.projectKey()));
      }
    }, null) != null;
  }

  private static String transition(Map<String, Object> properties) {
    String param = (String) properties.get("transition");
    if (Strings.isNullOrEmpty(param)) {
      throw new IllegalArgumentException("Missing parameter : 'transition'");
    }
    return param;
  }

}
