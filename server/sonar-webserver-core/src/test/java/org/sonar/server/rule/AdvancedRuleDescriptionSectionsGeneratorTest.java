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
package org.sonar.server.rule;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.server.rule.Context;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RuleDescriptionSectionBuilder;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;

@RunWith(MockitoJUnitRunner.class)
public class AdvancedRuleDescriptionSectionsGeneratorTest {
  private static final String UUID_1 = "uuid1";
  private static final String UUID_2 = "uuid2";

  private static final String HTML_CONTENT = "html content";

  private static final RuleDescriptionSection SECTION_1 = new RuleDescriptionSectionBuilder().sectionKey(HOW_TO_FIX_SECTION_KEY).htmlContent(HTML_CONTENT).build();
  private static final RuleDescriptionSection SECTION_2 = new RuleDescriptionSectionBuilder().sectionKey(ROOT_CAUSE_SECTION_KEY).htmlContent(HTML_CONTENT + "2").build();

  private static final Context CONTEXT_1 = new Context("ctx_1", "ctx 1 display name");
  private static final RuleDescriptionSection SECTION_3_WITH_CTX_1 = new RuleDescriptionSectionBuilder()
    .sectionKey(RESOURCES_SECTION_KEY)
    .htmlContent(HTML_CONTENT)
    .context(CONTEXT_1)
    .build();

  private static final Context CONTEXT_2 = new Context("ctx_2", "ctx 2 display name");
  private static final RuleDescriptionSection SECTION_3_WITH_CTX_2 = new RuleDescriptionSectionBuilder()
    .sectionKey(RESOURCES_SECTION_KEY)
    .htmlContent(HTML_CONTENT + "2")
    .context(CONTEXT_2)
    .build();

  private static final RuleDescriptionSectionDto EXPECTED_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key(HOW_TO_FIX_SECTION_KEY).content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto EXPECTED_SECTION_2 = RuleDescriptionSectionDto.builder().uuid(UUID_2).key(ROOT_CAUSE_SECTION_KEY)
    .content(HTML_CONTENT + "2").build();

  private static final RuleDescriptionSectionDto EXPECTED_SECTION_3_WITH_CTX_1 = RuleDescriptionSectionDto.builder()
    .uuid(UUID_1)
    .key(SECTION_3_WITH_CTX_1.getKey())
    .content(SECTION_3_WITH_CTX_1.getHtmlContent())
    .context(RuleDescriptionSectionContextDto.of(CONTEXT_1.getKey(), CONTEXT_1.getDisplayName()))
    .build();

  private static final RuleDescriptionSectionDto EXPECTED_SECTION_3_WITH_CTX_2 = RuleDescriptionSectionDto.builder()
    .uuid(UUID_2)
    .key(SECTION_3_WITH_CTX_2.getKey())
    .content(SECTION_3_WITH_CTX_2.getHtmlContent())
    .context(RuleDescriptionSectionContextDto.of(CONTEXT_2.getKey(), CONTEXT_2.getDisplayName()))
    .build();

  @Mock
  private UuidFactory uuidFactory;

  @Mock
  private RulesDefinition.Rule rule;

  @InjectMocks
  private AdvancedRuleDescriptionSectionsGenerator generator;

  @Before
  public void before() {
    when(uuidFactory.create()).thenReturn(UUID_1).thenReturn(UUID_2);
  }

  @Test
  public void generateSections_whenOneSection_createsOneSection() {
    when(rule.ruleDescriptionSections()).thenReturn(List.of(SECTION_1));

    Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = generator.generateSections(rule);

    assertThat(ruleDescriptionSectionDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(EXPECTED_SECTION_1);
  }

  @Test
  public void generateSections_whenTwoSections_createsTwoSections() {
    when(rule.ruleDescriptionSections()).thenReturn(List.of(SECTION_1, SECTION_2));

    Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = generator.generateSections(rule);

    assertThat(ruleDescriptionSectionDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(EXPECTED_SECTION_1, EXPECTED_SECTION_2);
  }

  @Test
  public void generateSections_whenTwoContextSpecificSections_createsTwoSections() {
    when(rule.ruleDescriptionSections()).thenReturn(List.of(SECTION_3_WITH_CTX_1, SECTION_3_WITH_CTX_2));

    Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = generator.generateSections(rule);

    assertThat(ruleDescriptionSectionDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(EXPECTED_SECTION_3_WITH_CTX_1, EXPECTED_SECTION_3_WITH_CTX_2);
  }

  @Test
  public void generateSections_whenContextSpecificSectionsAndNonContextSpecificSection_createsTwoSections() {
    when(rule.ruleDescriptionSections()).thenReturn(List.of(SECTION_1, SECTION_3_WITH_CTX_2));

    Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = generator.generateSections(rule);

    assertThat(ruleDescriptionSectionDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(EXPECTED_SECTION_1, EXPECTED_SECTION_3_WITH_CTX_2);
  }

}
