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

package org.sonar.wsclient.issue;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkChangeQueryTest {

  @Test
  public void get_all_issues() {
    BulkChangeQuery query = BulkChangeQuery.create();
    assertThat(query.urlParams()).isEmpty();
  }

  @Test
  public void test_create() {
    BulkChangeQuery query = BulkChangeQuery.create()
      .issues("ABCD", "EFGH")
      .actions("assign")
      .actionParameter("assign", "assignee", "geoffrey")
      .comment("My bulk comment")
      .sendNotifications(false);

    assertThat(query.urlParams()).hasSize(5)
      .containsEntry("issues", "ABCD,EFGH")
      .containsEntry("actions", "assign")
      .containsEntry("assign.assignee", "geoffrey")
      .containsEntry("comment", "My bulk comment")
      .containsEntry("sendNotifications", "false");
  }

  @Test
  public void should_not_add_null_issues() {
    BulkChangeQuery query = BulkChangeQuery.create()
      .issues(null);

    assertThat(query.urlParams()).isEmpty();
  }

  @Test
  public void should_not_add_null_actions() {
    BulkChangeQuery query = BulkChangeQuery.create()
      .actions(null);

    assertThat(query.urlParams()).isEmpty();
  }

}
