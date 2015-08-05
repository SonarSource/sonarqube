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
package org.sonar.server.issue.ws;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateActionTest {

  IssueService issueService = mock(IssueService.class);
  OperationResponseWriter responseWriter = mock(OperationResponseWriter.class);
  WsAction underTest = new CreateAction(issueService, responseWriter);
  WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void create_manual_issue_with_default_values() throws Exception {
    RuleKey ruleKey = RuleKey.of(RuleKey.MANUAL_REPOSITORY_KEY, "S1");
    when(issueService.createManualIssue("FILE_KEY", ruleKey, null, null, null))
      .thenReturn(new DefaultIssue().setKey("ISSUE_KEY"));

    tester.newRequest()
      .setParam("component", "FILE_KEY")
      .setParam("rule", ruleKey.toString())
      .execute();

    verify(issueService).createManualIssue("FILE_KEY", ruleKey, null, null, null);
    verify(responseWriter).write(eq("ISSUE_KEY"), any(Request.class), any(Response.class));
  }

  @Test
  public void create_manual_issue() throws Exception {
    RuleKey ruleKey = RuleKey.of(RuleKey.MANUAL_REPOSITORY_KEY, "S1");
    when(issueService.createManualIssue("FILE_KEY", ruleKey, 13, "the msg", "BLOCKER"))
      .thenReturn(new DefaultIssue().setKey("ISSUE_KEY"));

    tester.newRequest()
      .setParam("component", "FILE_KEY")
      .setParam("rule", ruleKey.toString())
      .setParam("severity", "BLOCKER")
      .setParam("line", "13")
      .setParam("message", "the msg")
      .execute();

    verify(issueService).createManualIssue("FILE_KEY", ruleKey, 13, "the msg", "BLOCKER");
    verify(responseWriter).write(eq("ISSUE_KEY"), any(Request.class), any(Response.class));
  }
}
