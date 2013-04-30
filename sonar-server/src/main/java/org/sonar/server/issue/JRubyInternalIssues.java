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
package org.sonar.server.issue;

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.platform.UserSession;

import java.util.List;
import java.util.Map;

/**
 * All the issue features that are not published to public API
 */
public class JRubyInternalIssues implements ServerComponent {

  private final ServerIssueActions actions;

  public JRubyInternalIssues(ServerIssueActions actions) {
    this.actions = actions;
  }

  public List<Transition> listTransitions(String issueKey) {
    return actions.listTransitions(issueKey, UserSession.get());
  }

  public Issue doTransition(String issueKey, String transitionKey) {
    return actions.doTransition(issueKey, transitionKey, UserSession.get());
  }

  public Issue assign(String issueKey, String transitionKey) {
    return actions.assign(issueKey, transitionKey, UserSession.get());
  }

  public Issue setSeverity(String issueKey, String severity) {
    return actions.setSeverity(issueKey, severity, UserSession.get());
  }

  public Issue create(Map<String, String> parameters) {
    String componentKey = parameters.get("component");
    // TODO verify authorization
    // TODO check existence of component
    DefaultIssueBuilder builder = new DefaultIssueBuilder().componentKey(componentKey);
    String line = parameters.get("line");
    builder.line(line != null ? Integer.parseInt(line) : null);
    builder.description(parameters.get("description"));
    builder.severity(parameters.get("severity"));
    // TODO verify existence of rule
    builder.ruleKey(RuleKey.parse(parameters.get("rule")));
    builder.manual(true);
    Issue issue = builder.build();
    return actions.create((DefaultIssue) issue, UserSession.get());
  }
}
