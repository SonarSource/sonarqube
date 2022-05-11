/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.issues;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sonar.core.util.issue.Issue;
import org.sonar.core.util.issue.IssueChangedEvent;
import org.sonar.server.pushapi.sonarlint.SonarLintClient;

import static java.util.Arrays.asList;

public class IssueChangeBroadcastUtils {
  private IssueChangeBroadcastUtils() {

  }

  public static Predicate<SonarLintClient> getFilterForEvent(IssueChangedEvent issueChangedEvent) {
    List<String> affectedProjects = asList(issueChangedEvent.getProjectKey());
    return client -> {
      Set<String> clientProjectKeys = client.getClientProjectKeys();
      return !Collections.disjoint(clientProjectKeys, affectedProjects);
    };
  }

  public static String getMessage(IssueChangedEvent issueChangedEvent) {
    return "event: " + issueChangedEvent.getEvent() + "\n"
      + "data: " + toJson(issueChangedEvent);
  }

  private static String toJson(IssueChangedEvent issueChangedEvent) {
    JSONObject data = new JSONObject();
    data.put("projectKey", issueChangedEvent.getProjectKey());

    JSONArray issuesJson = new JSONArray();
    for (Issue issue : issueChangedEvent.getIssues()) {
      issuesJson.put(toJson(issue));
    }
    data.put("issues", issuesJson);
    data.put("userSeverity", issueChangedEvent.getUserSeverity());
    data.put("userType", issueChangedEvent.getUserType());
    data.put("resolved", issueChangedEvent.getResolved());

    return data.toString();
  }

  private static JSONObject toJson(Issue issue) {
    JSONObject ruleJson = new JSONObject();
    ruleJson.put("issueKey", issue.getIssueKey());
    ruleJson.put("branchName", issue.getBranchName());
    return ruleJson;
  }

}
