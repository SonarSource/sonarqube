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
package org.sonar.server.rule.index;

import java.util.Set;
import org.junit.Test;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.server.security.SecurityStandards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleWithoutDescriptionSection;
import static org.sonar.markdown.Markdown.convertToHtml;
import static org.sonar.server.security.SecurityStandards.fromSecurityStandards;

public class RuleDocTest {

  @Test
  public void createFrom_mapsFieldCorrectly() {
    RuleDto ruleDto = newRule();
    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    ruleForIndexingDto.setTemplateRuleKey("templateKey");
    ruleForIndexingDto.setTemplateRepository("repoKey");
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);

    assertThat(ruleDoc.getId()).isEqualTo(ruleDto.getUuid());
    assertThat(ruleDoc.key()).isEqualTo(ruleForIndexingDto.getRuleKey());
    assertThat(ruleDoc.repository()).isEqualTo(ruleForIndexingDto.getRepository());
    assertThat(ruleDoc.internalKey()).isEqualTo(ruleForIndexingDto.getInternalKey());
    assertThat(ruleDoc.isExternal()).isEqualTo(ruleForIndexingDto.isExternal());
    assertThat(ruleDoc.language()).isEqualTo(ruleForIndexingDto.getLanguage());
    assertThat(ruleDoc.getCwe()).isEqualTo(securityStandards.getCwe());
    assertThat(ruleDoc.getOwaspTop10()).isEqualTo(securityStandards.getOwaspTop10());
    assertThat(ruleDoc.getOwaspTop10For2021()).isEqualTo(securityStandards.getOwaspTop10For2021());
    assertThat(ruleDoc.getSansTop25()).isEqualTo(securityStandards.getSansTop25());
    assertThat(ruleDoc.getSonarSourceSecurityCategory()).isEqualTo(securityStandards.getSqCategory());
    assertThat(ruleDoc.name()).isEqualTo(ruleForIndexingDto.getName());
    assertThat(ruleDoc.ruleKey()).isEqualTo(ruleForIndexingDto.getPluginRuleKey());
    assertThat(ruleDoc.severity()).isEqualTo(ruleForIndexingDto.getSeverityAsString());
    assertThat(ruleDoc.status()).isEqualTo(ruleForIndexingDto.getStatus());
    assertThat(ruleDoc.type().name()).isEqualTo(ruleForIndexingDto.getTypeAsRuleType().name());
    assertThat(ruleDoc.createdAt()).isEqualTo(ruleForIndexingDto.getCreatedAt());
    assertThat(ruleDoc.getTags())
      .containsAll(ruleForIndexingDto.getSystemTags())
      .containsAll(ruleForIndexingDto.getTags())
      .hasSize(ruleForIndexingDto.getSystemTags().size() + ruleForIndexingDto.getTags().size());
    assertThat(ruleDoc.updatedAt()).isEqualTo(ruleForIndexingDto.getUpdatedAt());
    assertThat(ruleDoc.templateKey().repository()).isEqualTo(ruleForIndexingDto.getTemplateRepository());
    assertThat(ruleDoc.templateKey().rule()).isEqualTo(ruleForIndexingDto.getTemplateRuleKey());

  }

  @Test
  public void createFrom_whenGivenNoHtmlSections_hasEmptyStringInHtmlDescription() {
    RuleDto ruleDto = newRuleWithoutDescriptionSection();
    ruleDto.setDescriptionFormat(RuleDto.Format.HTML);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription()).isEmpty();
  }

  @Test
  public void createFrom_whenGivenMultipleHtmlSections_hasConcatenationInHtmlDescription() {
    RuleDescriptionSectionDto section1 = buildRuleDescriptionSectionDto("section1", "<p>html content 1</p>");
    RuleDescriptionSectionDto section2 = buildRuleDescriptionSectionDto("section2", "<p>html content 2</p>");
    RuleDescriptionSectionDto section3ctx1 = buildRuleDescriptionSectionDtoWithContext("section3", "<p>html content 3.1</p>", "ctx1");
    RuleDescriptionSectionDto section3ctx2 = buildRuleDescriptionSectionDtoWithContext("section3", "<p>html content 3.2</p>", "ctx2");
    RuleDto ruleDto = newRule(section1, section2, section3ctx1, section3ctx2);
    ruleDto.setDescriptionFormat(RuleDto.Format.HTML);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription())
      .contains(section1.getContent())
      .contains(section2.getContent())
      .contains(section3ctx1.getContent())
      .contains(section3ctx2.getContent())
      .hasSameSizeAs(section1.getContent() + " " + section2.getContent() + " " + section3ctx1.getContent() + " " + section3ctx2.getContent());
  }

  @Test
  public void createFrom_whenGivenMultipleMarkdownSections_transformToHtmlAndConcatenatesInHtmlDescription() {
    RuleDescriptionSectionDto section1 = buildRuleDescriptionSectionDto("section1", "*html content 1*");
    RuleDescriptionSectionDto section2 = buildRuleDescriptionSectionDto("section2", "*html content 2*");

    RuleDto ruleDto = newRule(section1, section2);
    ruleDto.setDescriptionFormat(RuleDto.Format.MARKDOWN);

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);
    SecurityStandards securityStandards = fromSecurityStandards(ruleDto.getSecurityStandards());

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, securityStandards);
    assertThat(ruleDoc.htmlDescription())
      .contains(convertToHtml(section1.getContent()))
      .contains(convertToHtml(section2.getContent()))
      .hasSameSizeAs(convertToHtml(section1.getContent()) + " " + convertToHtml(section2.getContent()));
  }

  @Test
  public void createFrom_whenSecurityHotSpot_shouldNotPopulateCleanCodeAttribute() {
    RuleDto ruleDto = newRule();
    ruleDto.setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL);
    ruleDto.setType(RuleType.SECURITY_HOTSPOT.getDbConstant());

    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);

    SecurityStandards securityStandards = fromSecurityStandards(Set.of());
    Object field = RuleDoc.createFrom(ruleForIndexingDto, securityStandards).getNullableField(RuleIndexDefinition.FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY);
    assertThat(field).isNull();
  }

  @Test
  public void createFrom_whenAdHocRule_shouldPopulateWithAdHocType() {
    RuleDto ruleDto = newRule();
    ruleDto.setType(RuleType.CODE_SMELL);
    ruleDto.setIsAdHoc(true);
    ruleDto.setAdHocType(RuleType.BUG);
    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(ruleDto);

    RuleDoc ruleDoc = RuleDoc.createFrom(ruleForIndexingDto, fromSecurityStandards(Set.of()));

    assertThat(ruleDoc.type()).isEqualTo(RuleType.BUG);
  }

  private static RuleDescriptionSectionDto buildRuleDescriptionSectionDto(String key, String content) {
    return RuleDescriptionSectionDto.builder().key(key).content(content).build();
  }

  private static RuleDescriptionSectionDto buildRuleDescriptionSectionDtoWithContext(String key, String content, String contextKey) {
    return RuleDescriptionSectionDto.builder().key(key).content(content).context(RuleDescriptionSectionContextDto.of(contextKey, contextKey)).build();
  }
}
