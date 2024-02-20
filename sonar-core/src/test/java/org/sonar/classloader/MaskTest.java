/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.classloader;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MaskTest {

  @Test
  public void ALL_accepts_everything() throws Exception {
    assertThat(Mask.ALL.acceptClass("org.sonar.Bar")).isTrue();
    assertThat(Mask.ALL.acceptClass("Bar")).isTrue();
  }

  @Test
  public void NONE_accepts_nothing() throws Exception {
    assertThat(Mask.NONE.acceptClass("org.sonar.Bar")).isFalse();
    assertThat(Mask.NONE.acceptClass("Bar")).isFalse();
  }

  @Test
  public void include_class() throws Exception {
    Mask mask = Mask.builder().include("org/sonar/Bar.class").build();
    assertThat(mask.acceptClass("org.sonar.Bar")).isTrue();
    assertThat(mask.acceptClass("org.sonar.qube.Bar")).isFalse();
    assertThat(mask.acceptClass("org.sonar.Foo")).isFalse();
    assertThat(mask.acceptClass("Bar")).isFalse();
  }

  @Test
  public void include_class_of_root_package() throws Exception {
    Mask mask = Mask.builder().include("Bar.class").build();
    assertThat(mask.acceptClass("Bar")).isTrue();
    assertThat(mask.acceptClass("Foo")).isFalse();
  }

  @Test
  public void include_resource() throws Exception {
    Mask mask = Mask.builder().include("org/sonar/Bar.class").build();
    assertThat(mask.acceptResource("org/sonar/Bar.class")).isTrue();
    assertThat(mask.acceptResource("org/sonar/qube/Bar.class")).isFalse();
    assertThat(mask.acceptResource("org/sonar/Foo.class")).isFalse();
    assertThat(mask.acceptResource("Bar.class")).isFalse();
  }

  @Test
  public void include_package() throws Exception {
    Mask mask = Mask.builder().include("org/sonar/", "org/other/").build();
    assertThat(mask.acceptClass("Foo")).isFalse();
    assertThat(mask.acceptClass("org.sonar.Bar")).isTrue();
    assertThat(mask.acceptClass("org.sonarqube.Foo")).isFalse();
    assertThat(mask.acceptClass("org.sonar.qube.Foo")).isTrue();
    assertThat(mask.acceptClass("Bar")).isFalse();
  }

  @Test
  public void exclude_class() throws Exception {
    Mask mask = Mask.builder().exclude("org/sonar/Bar.class").build();
    assertThat(mask.acceptClass("org.sonar.Bar")).isFalse();
    assertThat(mask.acceptClass("org.sonar.qube.Bar")).isTrue();
    assertThat(mask.acceptClass("org.sonar.Foo")).isTrue();
    assertThat(mask.acceptClass("Bar")).isTrue();
  }

  @Test
  public void exclude_package() throws Exception {
    Mask mask = Mask.builder().exclude("org/sonar/", "org/other/").build();
    assertThat(mask.acceptClass("Foo")).isTrue();
    assertThat(mask.acceptClass("org.sonar.Bar")).isFalse();
    assertThat(mask.acceptClass("org.sonarqube.Foo")).isTrue();
    assertThat(mask.acceptClass("org.sonar.qube.Foo")).isFalse();
    assertThat(mask.acceptClass("Bar")).isTrue();
  }

  @Test
  public void exclusion_is_subset_of_inclusion() throws Exception {
    Mask mask = Mask.builder()
      .include("org/sonar/")
      .exclude("org/sonar/qube/")
      .build();
    assertThat(mask.acceptClass("org.sonar.Foo")).isTrue();
    assertThat(mask.acceptClass("org.sonar.Qube")).isTrue();
    assertThat(mask.acceptClass("org.sonar.qube.Foo")).isFalse();
  }

  @Test
  public void inclusion_is_subset_of_exclusion() throws Exception {
    Mask mask = Mask.builder()
      .include("org/sonar/qube/")
      .exclude("org/sonar/")
      .build();
    assertThat(mask.acceptClass("org.sonar.Foo")).isFalse();
    assertThat(mask.acceptClass("org.sonar.Qube")).isFalse();
    assertThat(mask.acceptClass("org.sonar.qube.Foo")).isFalse();
  }

  @Test
  public void exclude_everything() throws Exception {
    Mask mask = Mask.builder().exclude("/").build();
    assertThat(mask.acceptClass("org.sonar.Foo")).isFalse();
    assertThat(mask.acceptClass("Foo")).isFalse();
    assertThat(mask.acceptResource("config.xml")).isFalse();
    assertThat(mask.acceptResource("org/config.xml")).isFalse();
  }

  @Test
  public void include_everything() throws Exception {
    Mask mask = Mask.builder().include("/").build();
    assertThat(mask.acceptClass("org.sonar.Foo")).isTrue();
    assertThat(mask.acceptClass("Foo")).isTrue();
    assertThat(mask.acceptResource("config.xml")).isTrue();
    assertThat(mask.acceptResource("org/config.xml")).isTrue();
  }

  @Test
  public void merge_with_ALL() throws Exception {
    Mask mask = Mask.builder()
      .include("org/foo/")
      .exclude("org/bar/")
      .merge(Mask.ALL)
      .build();

    assertThat(mask.getInclusions()).containsOnly("org/foo/");
    assertThat(mask.getExclusions()).containsOnly("org/bar/");
  }

  @Test
  public void merge_exclusions() throws Exception {
    Mask with = Mask.builder().exclude("bar/").build();
    Mask mask = Mask.builder().exclude("org/foo/").merge(with).build();

    assertThat(mask.getExclusions()).containsOnly("org/foo/", "bar/");
  }

  @Test
  public void should_not_merge_disjoined_inclusions() throws Exception {
    Mask with = Mask.builder().include("org/bar/").build();
    Mask mask = Mask.builder().include("org/foo/").merge(with).build();

    assertThat(mask.getInclusions()).isEmpty();
    // TODO does that mean that merge result accepts everything ?
  }

  @Test
  public void merge_inclusions() throws Exception {
    Mask with = Mask.builder().include("org/foo/sub/", "org/bar/").build();
    Mask mask = Mask.builder().include("org/foo/", "org/bar/sub/").merge(with).build();

    assertThat(mask.getInclusions()).containsOnly("org/foo/sub/", "org/bar/sub/");
  }
}
