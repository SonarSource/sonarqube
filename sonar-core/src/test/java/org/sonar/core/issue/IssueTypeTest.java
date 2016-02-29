/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueTypeTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_valueOf_db_constant() {
    assertThat(IssueType.valueOf(1)).isEqualTo(IssueType.CODE_SMELL);
    assertThat(IssueType.valueOf(2)).isEqualTo(IssueType.BUG);
  }

  @Test
  public void valueOf_throws_ISE_if_unsupported_db_constant() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported value for db column ISSUES.ISSUE_TYPE: 4");
    IssueType.valueOf(4);
  }

  @Test
  public void test_ALL_NAMES() {
    assertThat(IssueType.ALL_NAMES).containsOnly("BUG", "VULNERABILITY", "CODE_SMELL");
  }

  @Test
  public void ALL_NAMES_is_immutable() {
    expectedException.expect(UnsupportedOperationException.class);
    IssueType.ALL_NAMES.add("foo");
  }
}
