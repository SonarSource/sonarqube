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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.sonar.core.util.Uuids;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.rule.RuleDto.ERROR_MESSAGE_SECTION_ALREADY_EXISTS;
import static org.sonar.db.rule.RuleTesting.newRule;

public class RuleDtoTest {


  public static final String SECTION_KEY = "section key";
  @Test
  public void fail_if_key_is_too_long() {
    assertThatThrownBy(() -> new RuleDto().setRuleKey(repeat("x", 250)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule key is too long: ");
  }

  @Test
  public void fail_if_name_is_too_long() {
    assertThatThrownBy(() -> new RuleDto().setName(repeat("x", 300)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule name is too long: ");
  }

  @Test
  public void fail_if_tags_are_too_long() {
    assertThatThrownBy(() -> {
      Set<String> tags = ImmutableSet.of(repeat("a", 2000), repeat("b", 1000), repeat("c", 2000));
      new RuleDto().setTags(tags);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule tags are too long: ");
  }

  @Test
  public void tags_are_optional() {
    RuleDto dto = new RuleDto().setTags(Collections.emptySet());
    assertThat(dto.getTags()).isEmpty();
  }

  @Test
  public void tags_are_joined_with_comma() {
    Set<String> tags = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setTags(tags);
    assertThat(dto.getTags()).isEqualTo(tags);
    assertThat(dto.getTagsAsString()).isEqualTo("first_tag,second_tag,third_tag");
  }

  @Test
  public void system_tags_are_joined_with_comma() {
    Set<String> systemTags = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setSystemTags(systemTags);
    assertThat(dto.getSystemTags()).isEqualTo(systemTags);
  }

  @Test
  public void security_standards_are_joined_with_comma() {
    Set<String> securityStandards = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setSecurityStandards(securityStandards);
    assertThat(dto.getSecurityStandards()).isEqualTo(securityStandards);
  }

  @Test
  public void equals_is_based_on_uuid() {
    String uuid = Uuids.createFast();
    RuleDto dto = newRule().setUuid(uuid);

    assertThat(dto)
      .isEqualTo(dto)
      .isEqualTo(newRule().setUuid(uuid))
      .isEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(uuid))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(Uuids.createFast()))
      .isNotEqualTo(newRule().setUuid(Uuids.createFast()));
  }

  @Test
  public void hashcode_is_based_on_uuid() {
    String uuid = Uuids.createFast();
    RuleDto dto = newRule().setUuid(uuid);

    assertThat(dto)
      .hasSameHashCodeAs(dto)
      .hasSameHashCodeAs(newRule().setUuid(uuid))
      .hasSameHashCodeAs(newRule().setRuleKey(dto.getRuleKey()).setUuid(uuid));
    assertThat(dto.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(newRule().setRuleKey(dto.getRuleKey()).setUuid(Uuids.createFast()).hashCode())
      .isNotEqualTo(newRule().setUuid(Uuids.createFast()).hashCode());
  }

  @Test
  public void add_rule_description_section_same_key_should_throw_error() {
    RuleDto dto = new RuleDto();

    RuleDescriptionSectionDto section1 = createSection(SECTION_KEY);
    dto.addRuleDescriptionSectionDto(section1);

    RuleDescriptionSectionDto section2 = createSection(SECTION_KEY);
    assertThatThrownBy(() -> dto.addRuleDescriptionSectionDto(section2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format(ERROR_MESSAGE_SECTION_ALREADY_EXISTS, SECTION_KEY, "null"));
  }

  @Test
  public void add_rule_description_section_with_different_context() {
    RuleDto dto = new RuleDto();

    RuleDescriptionSectionDto section1 = createSection(RuleDtoTest.SECTION_KEY, "context key 1", "context display Name 1");
    dto.addRuleDescriptionSectionDto(section1);

    RuleDescriptionSectionDto section2 = createSection(RuleDtoTest.SECTION_KEY, "context key 2", "context display Name 2");
    dto.addRuleDescriptionSectionDto(section2);

    assertThat(dto.getRuleDescriptionSectionDtos())
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(section1, section2);

  }

  @Test
  public void add_rule_description_section_with_same_section_and_context_should_throw_error() {
    RuleDto dto = new RuleDto();
    String contextKey = randomAlphanumeric(50);
    String displayName = randomAlphanumeric(50);
    RuleDescriptionSectionDto section1 = createSection(SECTION_KEY, contextKey, displayName);
    dto.addRuleDescriptionSectionDto(section1);
    RuleDescriptionSectionDto section2 = createSection(SECTION_KEY, contextKey, displayName);

    assertThatThrownBy(() -> dto.addRuleDescriptionSectionDto(section2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format(ERROR_MESSAGE_SECTION_ALREADY_EXISTS, SECTION_KEY, contextKey));

  }

  @NotNull
  private static RuleDescriptionSectionDto createSection(String section_key, String contextKey, String contextDisplayName) {
    return RuleDescriptionSectionDto.builder()
      .key(section_key)
      .context(RuleDescriptionSectionContextDto.of(contextKey, contextDisplayName))
      .build();
  }

  @NotNull
  private static RuleDescriptionSectionDto createSection(String section_key) {
    return RuleDescriptionSectionDto.builder()
      .key(section_key)
      .build();
  }
}
