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
package org.sonar.core.issue;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

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
  public void should_fail_on_bad_status() {
    try {
      issue.setStatus("FOO");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid status: FOO");
    }
  }

  @Test
  public void should_fail_on_bad_resolution() {
    try {
      issue.setResolution("FOO");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Not a valid resolution: FOO");
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
  public void description_should_be_abbreviated_if_too_long() {
    issue.setDescription(StringUtils.repeat("a", 5000));
    assertThat(issue.description()).hasSize(4000);
  }

  @Test
  public void description_should_be_trimmed() {
    issue.setDescription("    foo     ");
    assertThat(issue.description()).isEqualTo("foo");
  }

  @Test
  public void description_could_be_null() {
    issue.setDescription(null);
    assertThat(issue.description()).isNull();
  }
}
