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
package org.sonar.db.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectQgateAssociationQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void handle_underscore_and_percent() {
    ProjectQgateAssociationQuery underTest = ProjectQgateAssociationQuery.builder()
      .projectSearch("project-_%-search")
      .gateId("1").build();

    assertThat(underTest.projectSearchSql()).isEqualTo("project-\\_\\%-search%");
  }

  @Test
  public void fail_on_null_login() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Gate ID cannot be null");

    ProjectQgateAssociationQuery.Builder builder = ProjectQgateAssociationQuery.builder()
      .gateId(null);

    builder.build();
  }

  @Test
  public void fail_on_invalid_membership() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Membership is not valid (got unknown). Available values are [all, selected, deselected]");

    ProjectQgateAssociationQuery.Builder builder = ProjectQgateAssociationQuery.builder();
    builder.gateId("nelson");
    builder.membership("unknown");

    builder.build();
  }
}
