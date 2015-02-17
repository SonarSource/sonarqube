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
package org.sonar.wsclient.issue.internal;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.component.Component;
import org.sonar.wsclient.issue.BulkChange;
import org.sonar.wsclient.issue.IssueChange;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.rule.Rule;
import org.sonar.wsclient.unmarshallers.JsonUtils;
import org.sonar.wsclient.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @since 3.6
 */
public class IssueJsonParser {

  private static final String ISSUES = "issues";
  private static final String TOTAL = "total";

  public Issues parseIssues(String json) {
    DefaultIssues result = new DefaultIssues();
    Map jsonRoot = (Map) JSONValue.parse(json);
    List<Map> jsonIssues = (List<Map>) jsonRoot.get(ISSUES);
    if (jsonIssues != null) {
      for (Map jsonIssue : jsonIssues) {
        result.add(new DefaultIssue(jsonIssue));
      }
    }
    parseRules(result, jsonRoot);
    parseUsers(result, jsonRoot);
    parseComponents(result, jsonRoot);
    parseProjects(result, jsonRoot);
    parseActionPlans(result, jsonRoot);
    parsePaging(result, jsonRoot);
    return result;
  }

  private void parsePaging(DefaultIssues result, Map jsonRoot) {
    Map paging = (Map) jsonRoot.get("paging");
    result.setPaging(new Paging(paging));
  }

  private void parseProjects(DefaultIssues result, Map jsonRoot) {
    List<Map> jsonProjects = (List<Map>) jsonRoot.get("projects");
    if (jsonProjects != null) {
      for (Map jsonProject : jsonProjects) {
        result.addProject(new Component(jsonProject));
      }
    }
  }

  private void parseComponents(DefaultIssues result, Map jsonRoot) {
    List<Map> jsonComponents = (List<Map>) jsonRoot.get("components");
    if (jsonComponents != null) {
      for (Map jsonComponent : jsonComponents) {
        result.addComponent(new Component(jsonComponent));
      }
    }
  }

  private void parseUsers(DefaultIssues result, Map jsonRoot) {
    List<Map> jsonUsers = (List<Map>) jsonRoot.get("users");
    if (jsonUsers != null) {
      for (Map jsonUser : jsonUsers) {
        result.add(new User(jsonUser));
      }
    }
  }

  private void parseRules(DefaultIssues result, Map jsonRoot) {
    List<Map> jsonRules = (List<Map>) jsonRoot.get("rules");
    if (jsonRules != null) {
      for (Map jsonRule : jsonRules) {
        result.add(new Rule(jsonRule));
      }
    }
  }

  private void parseActionPlans(DefaultIssues result, Map jsonRoot) {
    List<Map> jsonRules = (List) jsonRoot.get("actionPlans");
    if (jsonRules != null) {
      for (Map jsonRule : jsonRules) {
        result.add(new DefaultActionPlan(jsonRule));
      }
    }
  }

  List<String> parseTransitions(String json) {
    List<String> transitions = new ArrayList<String>();
    Map jRoot = (Map) JSONValue.parse(json);
    List<String> jTransitions = (List<String>) jRoot.get("transitions");
    for (String jTransition : jTransitions) {
      transitions.add(jTransition);
    }
    return transitions;
  }

  List<IssueChange> parseChangelog(String json) {
    List<IssueChange> changes = new ArrayList<IssueChange>();
    Map jRoot = (Map) JSONValue.parse(json);
    List<Map> jChanges = (List<Map>) jRoot.get("changelog");
    if (jChanges != null) {
      for (Map jChange : jChanges) {
        changes.add(new DefaultIssueChange(jChange));
      }
    }
    return changes;
  }

  List<String> parseActions(String json) {
    List<String> actions = new ArrayList<String>();
    Map jRoot = (Map) JSONValue.parse(json);
    List<String> jActions = (List<String>) jRoot.get("actions");
    for (String jAction : jActions) {
      actions.add(jAction);
    }
    return actions;
  }

  BulkChange parseBulkChange(String json) {
    DefaultBulkChange result = new DefaultBulkChange();

    Map jsonRoot = (Map) JSONValue.parse(json);
    Map issuesChanged = (Map) jsonRoot.get("issuesChanged");
    result.setTotalIssuesChanged(JsonUtils.getInteger(issuesChanged, TOTAL));

    Map issuesNotChanged = (Map) jsonRoot.get("issuesNotChanged");
    result.setTotalIssuesNotChanged(JsonUtils.getInteger(issuesNotChanged, TOTAL));
    JSONArray issuesJson = JsonUtils.getArray(issuesNotChanged, ISSUES);
    if (issuesJson != null) {
      result.setIssuesNotChanged(issuesJson);
    }

    return result;
  }
}
