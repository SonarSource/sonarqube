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
package org.sonar.server.issue.notification;

import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueChangeNotificationTest {

  IssueChangeNotification notification = new IssueChangeNotification();

  @Test
  public void set_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCD")
      .setAssignee("simon")
      .setMessage("Remove this useless method")
      .setComponentKey("MyService")
      .setCurrentChange(new FieldDiffs().setDiff("resolution", "FALSE-POSITIVE", "FIXED"));

    IssueChangeNotification result = notification.setIssue(issue);

    assertThat(result.getFieldValue("key")).isEqualTo("ABCD");
    assertThat(result.getFieldValue("assignee")).isEqualTo("simon");
    assertThat(result.getFieldValue("message")).isEqualTo("Remove this useless method");
    assertThat(result.getFieldValue("old.resolution")).isEqualTo("FALSE-POSITIVE");
    assertThat(result.getFieldValue("new.resolution")).isEqualTo("FIXED");
  }

  @Test
  public void set_issue_with_current_change_having_no_old_value() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCD")
      .setAssignee("simon")
      .setMessage("Remove this useless method")
      .setComponentKey("MyService");

    IssueChangeNotification result = notification.setIssue(issue.setCurrentChange(new FieldDiffs().setDiff("resolution", null, "FIXED")));
    assertThat(result.getFieldValue("old.resolution")).isNull();
    assertThat(result.getFieldValue("new.resolution")).isEqualTo("FIXED");

    result = notification.setIssue(issue.setCurrentChange(new FieldDiffs().setDiff("resolution", "", "FIXED")));
    assertThat(result.getFieldValue("old.resolution")).isNull();
    assertThat(result.getFieldValue("new.resolution")).isEqualTo("FIXED");
  }

  @Test
  public void set_issue_with_current_change_having_no_new_value() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCD")
      .setAssignee("simon")
      .setMessage("Remove this useless method")
      .setComponentKey("MyService");

    IssueChangeNotification result = notification.setIssue(issue.setCurrentChange(new FieldDiffs().setDiff("assignee", "john", null)));
    assertThat(result.getFieldValue("old.assignee")).isEqualTo("john");
    assertThat(result.getFieldValue("new.assignee")).isNull();

    result = notification.setIssue(issue.setCurrentChange(new FieldDiffs().setDiff("assignee", "john", "")));
    assertThat(result.getFieldValue("old.assignee")).isEqualTo("john");
    assertThat(result.getFieldValue("new.assignee")).isNull();
  }

  @Test
  public void set_project_without_branch() {
    IssueChangeNotification result = notification.setProject("MyService", "My Service", null);
    assertThat(result.getFieldValue("projectKey")).isEqualTo("MyService");
    assertThat(result.getFieldValue("projectName")).isEqualTo("My Service");
    assertThat(result.getFieldValue("branch")).isNull();
  }

  @Test
  public void set_project_with_branch() {
    IssueChangeNotification result = notification.setProject("MyService", "My Service", "feature1");
    assertThat(result.getFieldValue("projectKey")).isEqualTo("MyService");
    assertThat(result.getFieldValue("projectName")).isEqualTo("My Service");
    assertThat(result.getFieldValue("branch")).isEqualTo("feature1");
  }

  @Test
  public void set_component() {
    IssueChangeNotification result = notification.setComponent(new ComponentDto().setDbKey("MyService").setLongName("My Service"));
    assertThat(result.getFieldValue("componentName")).isEqualTo("My Service");
    assertThat(result.getFieldValue("componentKey")).isEqualTo("MyService");
  }

  @Test
  public void set_change_author_login() {
    IssueChangeNotification result = notification.setChangeAuthorLogin("stephane");
    assertThat(result.getFieldValue("changeAuthor")).isEqualTo("stephane");
  }

  @Test
  public void set_rule_name() {
    IssueChangeNotification result = notification.setRuleName("Xoo Rule");
    assertThat(result.getFieldValue("ruleName")).isEqualTo("Xoo Rule");
  }

  @Test
  public void setComment() {
    IssueChangeNotification result = notification.setComment("My comment");
    assertThat(result.getFieldValue("comment")).isEqualTo("My comment");
  }
}
