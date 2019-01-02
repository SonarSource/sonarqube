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
package org.sonar.core.issue;

import com.google.common.collect.ImmutableMap;
import java.text.SimpleDateFormat;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DefaultIssueTest {

  private DefaultIssue issue = new DefaultIssue();

  @Test
  public void test_setters_and_getters() throws Exception {
    issue.setKey("ABCD")
      .setComponentKey("org.sample.Sample")
      .setProjectKey("Sample")
      .setRuleKey(RuleKey.of("squid", "S100"))
      .setLanguage("xoo")
      .setSeverity("MINOR")
      .setManualSeverity(true)
      .setMessage("a message")
      .setLine(7)
      .setGap(1.2d)
      .setEffort(Duration.create(28800L))
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setAssigneeUuid("julien")
      .setAuthorLogin("steph")
      .setChecksum("c7b5db46591806455cf082bb348631e8")
      .setNew(true)
      .setBeingClosed(true)
      .setOnDisabledRule(true)
      .setCopied(true)
      .setChanged(true)
      .setSendNotifications(true)
      .setCreationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"))
      .setUpdateDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-20"))
      .setCloseDate(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-21"))
      .setSelectedAt(1400000000000L);

    assertThat(issue.key()).isEqualTo("ABCD");
    assertThat(issue.componentKey()).isEqualTo("org.sample.Sample");
    assertThat(issue.projectKey()).isEqualTo("Sample");
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("squid", "S100"));
    assertThat(issue.language()).isEqualTo("xoo");
    assertThat(issue.severity()).isEqualTo("MINOR");
    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.message()).isEqualTo("a message");
    assertThat(issue.line()).isEqualTo(7);
    assertThat(issue.gap()).isEqualTo(1.2d);
    assertThat(issue.effort()).isEqualTo(Duration.create(28800L));
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.assignee()).isEqualTo("julien");
    assertThat(issue.authorLogin()).isEqualTo("steph");
    assertThat(issue.checksum()).isEqualTo("c7b5db46591806455cf082bb348631e8");
    assertThat(issue.isNew()).isTrue();
    assertThat(issue.isCopied()).isTrue();
    assertThat(issue.isBeingClosed()).isTrue();
    assertThat(issue.isOnDisabledRule()).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.mustSendNotifications()).isTrue();
    assertThat(issue.creationDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
    assertThat(issue.updateDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-20"));
    assertThat(issue.closeDate()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-21"));
    assertThat(issue.selectedAt()).isEqualTo(1400000000000L);
  }

  @Test
  public void set_empty_dates() {
    issue
      .setCreationDate(null)
      .setUpdateDate(null)
      .setCloseDate(null)
      .setSelectedAt(null);

    assertThat(issue.creationDate()).isNull();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.closeDate()).isNull();
    assertThat(issue.selectedAt()).isNull();
  }

  @Test
  public void test_attributes() {
    assertThat(issue.attribute("foo")).isNull();
    issue.setAttribute("foo", "bar");
    assertThat(issue.attribute("foo")).isEqualTo("bar");
    issue.setAttribute("foo", "newbar");
    assertThat(issue.attribute("foo")).isEqualTo("newbar");
    issue.setAttribute("foo", null);
    assertThat(issue.attribute("foo")).isNull();
  }

  @Test
  public void setAttributes_should_not_clear_existing_values() {
    issue.setAttributes(ImmutableMap.of("1", "one"));
    assertThat(issue.attribute("1")).isEqualTo("one");

    issue.setAttributes(ImmutableMap.of("2", "two"));
    assertThat(issue.attributes()).containsOnly(entry("1", "one"), entry("2", "two"));

    issue.setAttributes(null);
    assertThat(issue.attributes()).containsOnly(entry("1", "one"), entry("2", "two"));
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
    issue.setMessage(StringUtils.repeat("a", 5_000));
    assertThat(issue.message()).hasSize(1_333);
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
  public void test_nullable_fields() {
    issue.setGap(null).setSeverity(null).setLine(null);
    assertThat(issue.gap()).isNull();
    assertThat(issue.severity()).isNull();
    assertThat(issue.line()).isNull();
  }

  @Test
  public void test_equals_and_hashCode() {
    DefaultIssue a1 = new DefaultIssue().setKey("AAA");
    DefaultIssue a2 = new DefaultIssue().setKey("AAA");
    DefaultIssue b = new DefaultIssue().setKey("BBB");
    assertThat(a1).isEqualTo(a1);
    assertThat(a1).isEqualTo(a2);
    assertThat(a1).isNotEqualTo(b);
    assertThat(a1.hashCode()).isEqualTo(a1.hashCode());
  }

  @Test
  public void comments_should_not_be_modifiable() {
    DefaultIssue issue = new DefaultIssue().setKey("AAA");

    List<DefaultIssueComment> comments = issue.defaultIssueComments();
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
  public void all_changes_contain_current_change() {
    IssueChangeContext issueChangeContext = mock(IssueChangeContext.class);
    DefaultIssue issue = new DefaultIssue().setKey("AAA").setFieldChange(issueChangeContext, "actionPlan", "1.0", "1.1");

    assertThat(issue.changes()).hasSize(1);
  }

  @Test
  public void adding_null_change_has_no_effect() {
    DefaultIssue issue = new DefaultIssue();

    issue.addChange(null);

    assertThat(issue.changes()).hasSize(0);
  }
}
