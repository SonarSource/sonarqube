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
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.Uuids;
import org.sonar.db.issue.ImpactDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.rule.RuleDto.ERROR_MESSAGE_SECTION_ALREADY_EXISTS;
import static org.sonar.db.rule.RuleTesting.newRule;

class RuleDtoTest {

  private static final String SECTION_KEY = "section key";

  @Test
  void setRuleKey_whenTooLong_shouldFail() {
    assertThatThrownBy(() -> new RuleDto().setRuleKey(repeat("x", 250)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule key is too long: ");
  }

  @Test
  void setName_whenTooLong_shouldFail() {
    assertThatThrownBy(() -> new RuleDto().setName(repeat("x", 300)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule name is too long: ");
  }

  @Test
  void setTags_whenTooLong_shouldFail() {
    assertThatThrownBy(() -> {
      Set<String> tags = ImmutableSet.of(repeat("a", 25), repeat("b", 20), repeat("c", 41));
      new RuleDto().setTags(tags);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule tag is too long: ccccccccccccccccccccccccccccccccccccccccc");
  }

  @Test
  void setTags_shouldBeOptional() {
    RuleDto dto = new RuleDto().setTags(Collections.emptySet());
    assertThat(dto.getTags()).isEmpty();
  }

  @Test
  void setTags_shouldBeSet() {
    Set<String> tags = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setTags(tags);
    assertThat(dto.getTags()).isEqualTo(tags);
  }

  @Test
  void setSystemTags_shouldBeSet() {
    Set<String> systemTags = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setSystemTags(systemTags);
    assertThat(dto.getSystemTags()).isEqualTo(systemTags);
  }

  @Test
  void setSecurityStandards_shouldBeJoinedByCommas() {
    Set<String> securityStandards = new TreeSet<>(Set.of("first_tag", "second_tag", "third_tag"));
    RuleDto dto = new RuleDto().setSecurityStandards(securityStandards);
    assertThat(dto.getSecurityStandards()).isEqualTo(securityStandards);
  }

  @Test
  void equals_shouldBeBasedOnUuid() {
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
  void hashcode_shouldBeBasedOnUuid() {
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
  void addRuleDescriptionSectionDto_whenSameKey_shouldThrowError() {
    RuleDto dto = new RuleDto();

    RuleDescriptionSectionDto section1 = createSection(SECTION_KEY);
    dto.addRuleDescriptionSectionDto(section1);

    RuleDescriptionSectionDto section2 = createSection(SECTION_KEY);
    assertThatThrownBy(() -> dto.addRuleDescriptionSectionDto(section2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format(ERROR_MESSAGE_SECTION_ALREADY_EXISTS, SECTION_KEY, "null"));
  }

  @Test
  void addRuleDescriptionSectionDto_whenDifferentContext() {
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
  void addRuleDescriptionSectionDto_whenSameSectionAndContext_shouldThrowError() {
    RuleDto dto = new RuleDto();
    String contextKey = secure().nextAlphanumeric(50);
    String displayName = secure().nextAlphanumeric(50);
    RuleDescriptionSectionDto section1 = createSection(SECTION_KEY, contextKey, displayName);
    dto.addRuleDescriptionSectionDto(section1);
    RuleDescriptionSectionDto section2 = createSection(SECTION_KEY, contextKey, displayName);

    assertThatThrownBy(() -> dto.addRuleDescriptionSectionDto(section2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format(ERROR_MESSAGE_SECTION_ALREADY_EXISTS, SECTION_KEY, contextKey));
  }

  @Test
  void addDefaultImpact_whenSoftwareQualityAlreadyDefined_shouldThrowISE() {
    RuleDto dto = new RuleDto();
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.LOW));

    ImpactDto duplicatedImpact = newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH);

    assertThatThrownBy(() -> dto.addDefaultImpact(duplicatedImpact))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impact already defined on rule for Software Quality [MAINTAINABILITY]");
  }

  @Test
  void replaceAllDefaultImpacts_whenSoftwareQualityAlreadyDuplicated_shouldThrowISE() {
    RuleDto dto = new RuleDto();
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM));
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.SECURITY, Severity.HIGH));
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.RELIABILITY, Severity.LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH),
      newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.LOW));
    assertThatThrownBy(() -> dto.replaceAllDefaultImpacts(duplicatedImpacts))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impacts must have unique Software Quality values");
  }

  @Test
  void replaceAllImpacts_shouldReplaceExistingImpacts() {
    RuleDto dto = new RuleDto();
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM));
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.SECURITY, Severity.HIGH));
    dto.addDefaultImpact(newImpactDto(SoftwareQuality.RELIABILITY, Severity.LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(SoftwareQuality.MAINTAINABILITY, Severity.HIGH),
      newImpactDto(SoftwareQuality.SECURITY, Severity.LOW));

    dto.replaceAllDefaultImpacts(duplicatedImpacts);

    assertThat(dto.getDefaultImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(
        tuple(SoftwareQuality.MAINTAINABILITY, Severity.HIGH),
        tuple(SoftwareQuality.SECURITY, Severity.LOW));
  }

  @Test
  void getEnumType_shouldReturnCorrectValue() {
    RuleDto ruleDto = new RuleDto();
    ruleDto.setType(RuleType.CODE_SMELL);

    RuleType enumType = ruleDto.getEnumType();

    assertThat(enumType).isEqualTo(RuleType.CODE_SMELL);
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

  static ImpactDto newImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto(softwareQuality, severity);
  }
}
