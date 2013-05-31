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

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.fest.assertions.MapAssert.entry;

public class DefaultIssueTest {

  DefaultIssue issue = new DefaultIssue();

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
  public void should_fail_on_empty_status() {
    try {
      issue.setStatus("");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Status must be set");
    }
  }

  @Test
  public void should_fail_on_bad_severity() {
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
}
