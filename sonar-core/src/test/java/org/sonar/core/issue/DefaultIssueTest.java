/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
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
  public void size_of_description_should_be_limited() {
    try {
      issue.setDescription(StringUtils.repeat("a", 5000));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Description must not be longer than 4000 characters (got 5000)");
    }
  }
}
