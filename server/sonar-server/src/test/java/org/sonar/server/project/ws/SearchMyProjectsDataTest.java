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
package org.sonar.server.project.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.emptyList;

public class SearchMyProjectsDataTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SearchMyProjectsData.Builder underTest = SearchMyProjectsData.builder();

  @Test
  public void fail_if_projects_are_not_provided() {
    expectedException.expect(NullPointerException.class);

    underTest
      .setProjects(null)
      .setProjectLinks(emptyList())
      .setSnapshots(emptyList())
      .setQualityGates(emptyList())
      .setTotalNbOfProjects(0)
      .build();
  }

  @Test
  public void fail_if_projects_links_are_not_provided() {
    expectedException.expect(NullPointerException.class);

    underTest
      .setProjects(emptyList())
      .setProjectLinks(null)
      .setSnapshots(emptyList())
      .setQualityGates(emptyList())
      .setTotalNbOfProjects(0)
      .build();
  }

  @Test
  public void fail_if_snapshots_are_not_provided() {
    expectedException.expect(NullPointerException.class);

    underTest
      .setProjects(emptyList())
      .setProjectLinks(emptyList())
      .setSnapshots(null)
      .setQualityGates(emptyList())
      .setTotalNbOfProjects(0)
      .build();
  }

  @Test
  public void fail_if_quality_gates_are_not_provided() {
    expectedException.expect(NullPointerException.class);

    underTest
      .setProjects(emptyList())
      .setProjectLinks(emptyList())
      .setSnapshots(emptyList())
      .setQualityGates(null)
      .build();
  }

  @Test
  public void fail_if_total_number_of_projects_is_not_provided() {
    expectedException.expect(NullPointerException.class);

    underTest
      .setProjects(emptyList())
      .setProjectLinks(emptyList())
      .setSnapshots(emptyList())
      .setQualityGates(emptyList())
      .setTotalNbOfProjects(null)
      .build();
  }
}
