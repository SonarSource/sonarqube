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

package org.sonar.server.issue;

import org.junit.Test;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssueBulkChangeQueryTest {

  @Test
  public void test_build(){
    IssueBulkChangeQuery issueBulkChangeQuery = IssueBulkChangeQuery.builder().issueKeys(newArrayList("ABCD", "EFGH")).assignee("perceval").build();
    assertThat(issueBulkChangeQuery.issueKeys()).isNotNull();
    assertThat(issueBulkChangeQuery.assignee()).isNotNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_build_if_no_issue(){
    IssueBulkChangeQuery.builder().assignee("perceval").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_build_if_issues_are_empty(){
    IssueBulkChangeQuery.builder().issueKeys(Collections.<String>emptyList()).assignee("perceval").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_to_build_if_no_action(){
    IssueBulkChangeQuery.builder().issueKeys(newArrayList("ABCD", "EFGH")).build();
  }

}
