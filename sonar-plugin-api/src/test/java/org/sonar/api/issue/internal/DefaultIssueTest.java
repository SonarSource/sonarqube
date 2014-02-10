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
package org.sonar.api.issue.internal;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WorkUnit;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Mockito.mock;

public class DefaultIssueTest {

  DefaultIssue issue = new DefaultIssue();

  @Test
  public void test_setters_and_getters() throws Exception {
    issue.setKey("ABCD")
      .setComponentKey("org.sample.Sample")
      .setProjectKey("Sample")
      .setRuleKey(RuleKey.of("squid", "S100"))
      .setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("a message")
      .setLine(7)
      .setEffortToFix(1.2d)
      .setTechnicalDebt(new WorkUnit.Builder().setDays(1).build())
      .setActionPlanKey("BCDE")
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setReporter("simon")
      .setAssignee("julien")
      .setAuthorLogin("steph")
      .setChecksum("c7b5db46591806455cf082bb348631e8")
      .setNew(true)
      .setEndOfLife(true)
      .setOnDisabledRule(true)
      .setChanged(true)
      .setSendNotifications(true)
      .setCreationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"))
      .setUpdateDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-20"))
      .setCloseDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-21"))
      .setSelectedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-22"))
    ;

    assertThat(issue.key()).isEqualTo("ABCD");
    assertThat(issue.componentKey()).isEqualTo("org.sample.Sample");
    assertThat(issue.projectKey()).isEqualTo("Sample");
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("squid", "S100"));
    assertThat(issue.severity()).isEqualTo("MINOR");
    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.message()).isEqualTo("a message");
    assertThat(issue.line()).isEqualTo(7);
    assertThat(issue.effortToFix()).isEqualTo(1.2d);
    assertThat(issue.technicalDebt()).isEqualTo(new WorkUnit.Builder().setDays(1).build());
    assertThat(issue.actionPlanKey()).isEqualTo("BCDE");
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.reporter()).isEqualTo("simon");
    assertThat(issue.assignee()).isEqualTo("julien");
    assertThat(issue.authorLogin()).isEqualTo("steph");
    assertThat(issue.checksum()).isEqualTo("c7b5db46591806455cf082bb348631e8");
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.isEndOfLife()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.mustSendNotifications()).isTrue();
    assertThat(issue.creationDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
    assertThat(issue.updateDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-20"));
    assertThat(issue.closeDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-21"));
    assertThat(issue.selectedAt()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-22"));
  }

  @Test
  public void set_empty_dates() throws Exception {
    issue
      .setCreationDate(null)
      .setUpdateDate(null)
      .setCloseDate(null)
      .setSelectedAt(null)
    ;

    assertThat(issue.creationDate()).isNull();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.selectedAt()).isNull();
  }

  @Test
  public void test_attributes() throws Exception {
    assertThat(issue.attribute("foo")).isNull();
    issue.setAttribute("foo", "bar");
    assertThat(issue.attribute("foo")).isEqualTo("bar");
    issue.setAttribute("foo", "newbar");
    assertThat(issue.attribute("foo")).isEqualTo("newbar");
    issue.setAttribute("foo", null);
    assertThat(issue.attribute("foo")).isNull();
  }

  @Test
  public void setAttributes_should_not_clear_existing_values() throws Exception {
    issue.setAttributes(ImmutableMap.of("1", "one"));
    assertThat(issue.attribute("1")).isEqualTo("one");

    issue.setAttributes(ImmutableMap.of("2", "two"));
    assertThat(issue.attributes()).hasSize(2);
    assertThat(issue.attributes()).includes(entry("1", "one"), entry("2", "two"));

    issue.setAttributes(null);
    assertThat(issue.attributes()).hasSize(2);
    assertThat(issue.attributes()).includes(entry("1", "one"), entry("2", "two"));
  }

  @Test
  public void fail_on_empty_status() {
    try {
      issue.setStatus("");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Status must be set");
    }
  }

  @Test
  public void fail_on_bad_severity() {
    try {
      issue.setSeverity("FOO");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid severity: FOO");
    }
  }

  @Test
  public void message_should_be_abbreviated_if_too_long() {
    issue.setMessage(StringUtils.repeat("a", 5000));
    assertThat(issue.message()).hasSize(4000);
  }

  @Test
  public void message_should_be_trimmed() {
    issue.setMessage("    foo     ");
    assertThat(issue.message()).isEqualTo("foo");
  }

  @Test
  public void message_could_be_null() {
    issue.setMessage(null);
    assertThat(issue.message()).isNull();
  }

  @Test
  public void test_nullable_fields() throws Exception {
    issue.setEffortToFix(null).setSeverity(null).setLine(null);
    assertThat(issue.effortToFix()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.line()).isNull();
  }

  @Test
  public void test_equals_and_hashCode() throws Exception {
    DefaultIssue a1 = new DefaultIssue().setKey("AAA");
    DefaultIssue a2 = new DefaultIssue().setKey("AAA");
    DefaultIssue b = new DefaultIssue().setKey("BBB");
    assertThat(a1).isEqualTo(a1);
    assertThat(a1).isEqualTo(a2);
    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isEqualTo(a1.hashCode());
  }

  @Test
  public void comments_should_not_be_modifiable() throws Exception {
    DefaultIssue issue = new DefaultIssue().setKey("AAA");

    List<IssueComment> comments = issue.comments();
    assertThat(comments).isEmpty();

    try {
      comments.add(new DefaultIssueComment());
      fail();
    } catch (UnsupportedOperationException e) {
      // ok
    } catch (Exception e) {
      fail("Unexpected exception: " + e);
    }
  }

  @Test
  public void all_changes_contain_current_change() throws Exception {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    DefaultIssue issue = new DefaultIssue().setKey("AAA").setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");

    assertThat(issue.changes()).hasSize(1);
  }
}
