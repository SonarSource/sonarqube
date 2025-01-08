/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.dependency;

import org.junit.jupiter.api.Test;

import static com.google.common.base.Strings.repeat;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectDependencyImplTest {

  static final String KEY = "KEY";
  static final String UUID = "UUID";

  @Test
  void builder_shouldSetKeyUuidAndName() {
    ProjectDependencyImpl dep = buildSimpleDependency(KEY).setUuid(UUID).setName("name").build();

    assertThat(dep.getKey()).isEqualTo(KEY);
    assertThat(dep.getUuid()).isEqualTo(UUID);
    assertThat(dep.getName()).isEqualTo("name");
  }

  @Test
  void builder_shouldKeep500FirstCharactersOfName() {
    String veryLongString = repeat("a", 3_000);

    ProjectDependencyImpl underTest = buildSimpleDependency("dep")
      .setFullName(veryLongString)
      .build();

    String expectedName = repeat("a", 500 - 3) + "...";
    assertThat(underTest.getFullName()).isEqualTo(expectedName);
  }

  @Test
  void builder_shouldKeep2000FirstCharactersOfDescription() {
    String veryLongString = repeat("a", 3_000);

    ProjectDependencyImpl underTest = buildSimpleDependency("dep")
      .setDescription(veryLongString)
      .build();

    String expectedDescription = repeat("a", 2_000 - 3) + "...";
    assertThat(underTest.getDescription()).isEqualTo(expectedDescription);
  }

  @Test
  void builder_shouldSetOptionalVersionAndPackageManager() {
    ProjectDependencyImpl underTest = buildSimpleDependency("dep")
      .setVersion("1.0")
      .setPackageManager("mvn")
      .build();

    assertThat(underTest.getVersion()).isEqualTo("1.0");
    assertThat(underTest.getPackageManager()).isEqualTo("mvn");
  }

  @Test
  void equals_shouldComparesOnUuidOnly() {
    ProjectDependencyImpl.Builder builder = buildSimpleDependency("1").setUuid(UUID);

    assertThat(builder.build()).isEqualTo(builder.build());
    assertThat(builder.build()).isEqualTo(buildSimpleDependency("2").setUuid(UUID).build());
    assertThat(builder.build()).isNotEqualTo(buildSimpleDependency("1").setUuid("otherUUid").build());
  }

  @Test
  void hashCode_shouldBeHashcodeOfUuid() {
    ProjectDependencyImpl.Builder builder = buildSimpleDependency("1").setUuid(UUID);

    assertThat(builder.build()).hasSameHashCodeAs(builder.build().hashCode());
    assertThat(builder.build()).hasSameHashCodeAs(buildSimpleDependency("2").setUuid(UUID).build().hashCode());
    assertThat(builder.build()).hasSameHashCodeAs(UUID.hashCode());
  }

  private static ProjectDependencyImpl.Builder buildSimpleDependency(String key) {
    return ProjectDependencyImpl.builder()
      .setName("name_" + key)
      .setKey(key)
      .setUuid("uuid_" + key);
  }
}
