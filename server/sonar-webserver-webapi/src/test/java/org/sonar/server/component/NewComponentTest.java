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
package org.sonar.server.component;

import org.junit.Test;
import org.sonar.server.common.component.NewComponent;

import static com.google.common.base.Strings.repeat;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.server.common.component.NewComponent.newComponentBuilder;

public class NewComponentTest {
  private static final String KEY = "key";
  private static final String NAME = "name";

  private final NewComponent.Builder underTest = newComponentBuilder();

  @Test
  public void build_throws_IAE_when_key_is_null() {
    underTest.setKey(null);

    expectBuildException(IllegalArgumentException.class, "Component key can't be empty");
  }

  @Test
  public void build_throws_IAE_when_key_is_empty() {
    underTest
      .setKey("");

    expectBuildException(IllegalArgumentException.class, "Component key can't be empty");
  }

  @Test
  public void build_throws_IAE_when_key_is_longer_than_400_characters() {
    underTest.setKey(repeat("a", 400 + 1));

    expectBuildException(
      IllegalArgumentException.class,
      "Component key length (401) is longer than the maximum authorized (400)");
  }

  @Test
  public void build_fails_with_IAE_when_name_is_null() {
    underTest.setKey(KEY);

    expectBuildException(IllegalArgumentException.class, "Component name can't be empty");
  }

  @Test
  public void build_fails_with_IAE_when_name_is_empty() {
    underTest.setKey(KEY)
      .setName("");

    expectBuildException(IllegalArgumentException.class, "Component name can't be empty");
  }

  @Test
  public void build_fails_with_IAE_when_name_is_longer_than_2000_characters() {
    underTest.setKey(KEY)
      .setName(repeat("a", 501));

    expectBuildException(
      IllegalArgumentException.class,
      "Component name length (501) is longer than the maximum authorized (500)");
  }

  @Test
  public void build_fails_with_IAE_when_qualifier_is_null() {
    underTest.setKey(KEY)
      .setName(NAME)
      .setQualifier(null);

    expectBuildException(IllegalArgumentException.class, "Component qualifier can't be empty");
  }

  @Test
  public void build_fails_with_IAE_when_qualifier_is_empty() {
    underTest.setKey(KEY)
      .setName(NAME)
      .setQualifier("");

    expectBuildException(IllegalArgumentException.class, "Component qualifier can't be empty");
  }

  @Test
  public void build_fails_with_IAE_when_qualifier_is_longer_than_10_characters() {
    underTest.setKey(KEY)
      .setName(NAME)
      .setQualifier(repeat("a", 10 + 1));

    expectBuildException(
      IllegalArgumentException.class,
      "Component qualifier length (11) is longer than the maximum authorized (10)");
  }

  @Test
  public void getQualifier_returns_PROJECT_when_no_set_in_builder() {
    NewComponent newComponent = underTest.setKey(KEY)
      .setName(NAME)
      .build();

    assertThat(newComponent.qualifier()).isEqualTo(PROJECT);
  }

  @Test
  public void isProject_shouldReturnTrue_whenQualifierIsProject() {
    NewComponent newComponent = underTest.setKey(KEY)
      .setName(NAME)
      .setQualifier(PROJECT)
      .build();

    assertThat(newComponent.isProject()).isTrue();
  }

  @Test
  public void isProject_shouldReturnFalse_whenQualifierIsNotProject() {
    NewComponent newComponent = underTest.setKey(KEY)
      .setName(NAME)
      .setQualifier(secure().nextAlphabetic(4))
      .build();

    assertThat(newComponent.isProject()).isFalse();
  }

  private void expectBuildException(Class<? extends Exception> expectedExceptionType, String expectedMessage) {
    assertThatThrownBy(underTest::build)
      .isInstanceOf(expectedExceptionType)
      .hasMessageContaining(expectedMessage);
  }
}
