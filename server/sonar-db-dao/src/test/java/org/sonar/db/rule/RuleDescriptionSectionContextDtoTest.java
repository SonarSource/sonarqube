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
package org.sonar.db.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.rule.RuleDescriptionSectionContextDto.DISPLAY_NAME_MUST_BE_SET_ERROR;
import static org.sonar.db.rule.RuleDescriptionSectionContextDto.KEY_MUST_BE_SET_ERROR;

class RuleDescriptionSectionContextDtoTest {

  private static final String CONTEXT_KEY = "key";
  private static final String CONTEXT_DISPLAY_NAME = "displayName";

  @Test
  void check_of_instantiate_object() {
    RuleDescriptionSectionContextDto context = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);

    assertThat(context).extracting(RuleDescriptionSectionContextDto::getKey,
      RuleDescriptionSectionContextDto::getDisplayName).contains(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
  }

  @Test
  void check_of_with_key_is_empty() {
    assertThatThrownBy(() -> RuleDescriptionSectionContextDto.of("", CONTEXT_DISPLAY_NAME))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(KEY_MUST_BE_SET_ERROR);
  }

  @Test
  void check_of_with_display_name_is_empty() {
    assertThatThrownBy(() -> RuleDescriptionSectionContextDto.of(CONTEXT_KEY, ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(DISPLAY_NAME_MUST_BE_SET_ERROR);
  }

  @Test
  void equals_with_equals_objects_should_return_true() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    RuleDescriptionSectionContextDto context2 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    assertThat(context1).isEqualTo(context2);
  }

  @Test
  void equals_with_same_objects_should_return_true() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    assertThat(context1).isEqualTo(context1);
  }

  @Test
  void equals_with_one_null_objet_should_return_false() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    assertThat(context1).isNotEqualTo(null);
  }

  @Test
  void equals_with_different_display_names_should_return_false() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    RuleDescriptionSectionContextDto context2 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME + "2");
    assertThat(context1).isNotEqualTo(context2);
  }

  @Test
  void equals_with_different_context_keys_should_return_false() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    RuleDescriptionSectionContextDto context2 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY + "2", CONTEXT_DISPLAY_NAME);
    assertThat(context1).isNotEqualTo(context2);
  }

  @Test
  void hashcode_with_equals_objects_should_return_same_hash() {
    RuleDescriptionSectionContextDto context1 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    RuleDescriptionSectionContextDto context2 = RuleDescriptionSectionContextDto.of(CONTEXT_KEY, CONTEXT_DISPLAY_NAME);
    assertThat(context1).hasSameHashCodeAs(context2);
  }


}
