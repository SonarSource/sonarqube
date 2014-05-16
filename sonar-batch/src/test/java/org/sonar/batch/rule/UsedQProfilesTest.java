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
package org.sonar.batch.rule;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class UsedQProfilesTest {

  @Test
  public void serialization() throws Exception {

    ModuleQProfiles.QProfile java = new ModuleQProfiles.QProfile(1, "Sonar Way", "java", 1);
    ModuleQProfiles.QProfile php = new ModuleQProfiles.QProfile(2, "Sonar Way", "php", 1);

    UsedQProfiles used = UsedQProfiles.fromProfiles(java, php);
    assertThat(used.toJSON()).isEqualTo(
      "[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");
  }

  @Test
  public void deserialization() throws Exception {
    UsedQProfiles used = UsedQProfiles
      .fromJSON("[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");

    assertThat(used.toJSON()).isEqualTo(
      "[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");
  }

  @Test
  public void merge() throws Exception {
    UsedQProfiles first = UsedQProfiles
      .fromJSON("[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"}]");

    UsedQProfiles second = UsedQProfiles
      .fromJSON("[{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");

    assertThat(first.merge(second).toJSON()).isEqualTo(
      "[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");
  }

  @Test
  public void merge_no_duplicate_ids() throws Exception {
    UsedQProfiles first = UsedQProfiles
      .fromJSON("[{\"id\":1,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":2,\"language\":\"php\"}]");

    UsedQProfiles second = UsedQProfiles
      .fromJSON("[{\"id\":1,\"name\":\"Sonar Way\",\"version\":2,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":1,\"language\":\"php\"}]");

    assertThat(first.merge(second).toJSON()).isEqualTo(
      "[{\"id\":1,\"name\":\"Sonar Way\",\"version\":2,\"language\":\"java\"},{\"id\":2,\"name\":\"Sonar Way\",\"version\":2,\"language\":\"php\"}]");
  }

}
