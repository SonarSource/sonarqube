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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.api.server.rule.RuleDescriptionSectionBuilder;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.server.rule.RuleDescriptionGeneratorTestData.RuleDescriptionSectionGeneratorIdentifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.server.rule.RuleDescriptionGeneratorTestData.RuleDescriptionSectionGeneratorIdentifier.ADVANCED_RULE;
import static org.sonar.server.rule.RuleDescriptionGeneratorTestData.RuleDescriptionSectionGeneratorIdentifier.LEGACY_HOTSPOT;
import static org.sonar.server.rule.RuleDescriptionGeneratorTestData.RuleDescriptionSectionGeneratorIdentifier.LEGACY_ISSUE;
import static org.sonar.server.rule.RuleDescriptionGeneratorTestData.aRuleOfType;

@RunWith(Parameterized.class)
public class RuleDescriptionSectionsGeneratorsTest {

  private static final String KEY_1 = "key";
  private static final String KEY_2 = "key_2";
  private static final String UUID_1 = "uuid1";
  private static final String UUID_2 = "uuid2";

  private static final String HTML_CONTENT = "html content";
  private static final String MD_CONTENT = "md content balblab";

  private static final RuleDescriptionSection SECTION_1 = new RuleDescriptionSectionBuilder().sectionKey(KEY_1).htmlContent(HTML_CONTENT).build();
  private static final RuleDescriptionSection SECTION_2 = new RuleDescriptionSectionBuilder().sectionKey(KEY_2).htmlContent(HTML_CONTENT).build();

  private static final RuleDescriptionSectionDto DEFAULT_HTML_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key("default").content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto DEFAULT_HTML_HOTSPOT_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key(ROOT_CAUSE_SECTION_KEY).content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto DEFAULT_MD_HOTSPOT_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key(ROOT_CAUSE_SECTION_KEY).content(MD_CONTENT).build();
  private static final RuleDescriptionSectionDto DEFAULT_MD_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key("default").content(MD_CONTENT).build();
  private static final RuleDescriptionSectionDto HTML_SECTION_1 = RuleDescriptionSectionDto.builder().uuid(UUID_1).key(KEY_1).content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto HTML_SECTION_2 = RuleDescriptionSectionDto.builder().uuid(UUID_2).key(KEY_2).content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto LEGACY_HTML_SECTION = RuleDescriptionSectionDto.builder().uuid(UUID_2).key("default").content(HTML_CONTENT).build();
  private static final RuleDescriptionSectionDto LEGACY_MD_SECTION = RuleDescriptionSectionDto.builder().uuid(UUID_2).key("default").content(MD_CONTENT).build();

  @Parameterized.Parameters(name = "{index} = {0}")
  public static List<RuleDescriptionGeneratorTestData> testData() {
    return Arrays.asList(
      // ISSUES WITHOUT SECTIONS
      aRuleOfType(BUG).html(null).md(null).expectedGenerator(LEGACY_ISSUE).build(),
      aRuleOfType(BUG).html(HTML_CONTENT).md(null).expectedGenerator(LEGACY_ISSUE).addExpectedSection(DEFAULT_HTML_SECTION_1).build(),
      aRuleOfType(BUG).html(null).md(MD_CONTENT).expectedGenerator(LEGACY_ISSUE).addExpectedSection(DEFAULT_MD_SECTION_1).build(),
      aRuleOfType(BUG).html(HTML_CONTENT).md(MD_CONTENT).expectedGenerator(LEGACY_ISSUE).addExpectedSection(DEFAULT_HTML_SECTION_1).build(),
      aRuleOfType(CODE_SMELL).html(HTML_CONTENT).md(MD_CONTENT).expectedGenerator(LEGACY_ISSUE).addExpectedSection(DEFAULT_HTML_SECTION_1).build(),
      aRuleOfType(VULNERABILITY).html(HTML_CONTENT).md(MD_CONTENT).expectedGenerator(LEGACY_ISSUE).addExpectedSection(DEFAULT_HTML_SECTION_1).build(),
      // HOTSPOT WITHOUT SECTIONS
      aRuleOfType(SECURITY_HOTSPOT).html(null).md(null).expectedGenerator(LEGACY_HOTSPOT).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(HTML_CONTENT).md(null).expectedGenerator(LEGACY_HOTSPOT).addExpectedSection(DEFAULT_HTML_HOTSPOT_SECTION_1).addExpectedSection(LEGACY_HTML_SECTION).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(null).md(MD_CONTENT).expectedGenerator(LEGACY_HOTSPOT).addExpectedSection(DEFAULT_MD_HOTSPOT_SECTION_1).addExpectedSection(LEGACY_MD_SECTION).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(HTML_CONTENT).md(MD_CONTENT).expectedGenerator(LEGACY_HOTSPOT).addExpectedSection(DEFAULT_HTML_HOTSPOT_SECTION_1).addExpectedSection(LEGACY_HTML_SECTION).build(),
      // RULES WITH SECTIONS
      aRuleOfType(BUG).html(null).md(null).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(BUG).html(HTML_CONTENT).md(null).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(BUG).html(null).md(MD_CONTENT).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(BUG).html(HTML_CONTENT).md(MD_CONTENT).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(BUG).html(HTML_CONTENT).md(MD_CONTENT).addSection(SECTION_1).addSection(SECTION_2).expectedGenerator(ADVANCED_RULE)
        .addExpectedSection(HTML_SECTION_1).addExpectedSection(HTML_SECTION_2).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(null).md(null).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(HTML_CONTENT).md(null).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(null).md(MD_CONTENT).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(HTML_CONTENT).md(MD_CONTENT).addSection(SECTION_1).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).build(),
      aRuleOfType(SECURITY_HOTSPOT).html(HTML_CONTENT).md(MD_CONTENT).addSection(SECTION_1).addSection(SECTION_2).expectedGenerator(ADVANCED_RULE).addExpectedSection(HTML_SECTION_1).addExpectedSection(HTML_SECTION_2).build()
    );
  }

  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final RulesDefinition.Rule rule = mock(RulesDefinition.Rule.class);

  private final RuleDescriptionGeneratorTestData testData;

  private final RuleDescriptionSectionsGenerator legacyHotspotRuleDescriptionSectionsGenerator = new LegacyHotspotRuleDescriptionSectionsGenerator(uuidFactory);
  private final LegacyIssueRuleDescriptionSectionsGenerator legacyIssueRuleDescriptionSectionsGenerator = new LegacyIssueRuleDescriptionSectionsGenerator(uuidFactory);
  private final RuleDescriptionSectionsGenerator advancedRuleDescriptionSectionsGenerator = new AdvancedRuleDescriptionSectionsGenerator(uuidFactory);

  Map<RuleDescriptionSectionGeneratorIdentifier, RuleDescriptionSectionsGenerator> idToGenerator = ImmutableMap.<RuleDescriptionSectionGeneratorIdentifier, RuleDescriptionSectionsGenerator>builder()
    .put(ADVANCED_RULE, advancedRuleDescriptionSectionsGenerator)
    .put(LEGACY_HOTSPOT, legacyHotspotRuleDescriptionSectionsGenerator)
    .put(LEGACY_ISSUE, legacyIssueRuleDescriptionSectionsGenerator)
    .build();

  public RuleDescriptionSectionsGeneratorsTest(RuleDescriptionGeneratorTestData testData) {
    this.testData = testData;
  }

  @Before
  public void before() {
    when(uuidFactory.create()).thenReturn(UUID_1).thenReturn(UUID_2);
    when(rule.htmlDescription()).thenReturn(testData.getHtmlDescription());
    when(rule.markdownDescription()).thenReturn(testData.getMarkdownDescription());
    when(rule.ruleDescriptionSections()).thenReturn(testData.getRuleDescriptionSections());
    when(rule.type()).thenReturn(testData.getRuleType());
  }

  @Test
  public void scenario() {
    assertThat(advancedRuleDescriptionSectionsGenerator.isGeneratorForRule(rule)).isEqualTo(ADVANCED_RULE.equals(testData.getExpectedGenerator()));
    assertThat(legacyHotspotRuleDescriptionSectionsGenerator.isGeneratorForRule(rule)).isEqualTo(LEGACY_HOTSPOT.equals(testData.getExpectedGenerator()));
    assertThat(legacyIssueRuleDescriptionSectionsGenerator.isGeneratorForRule(rule)).isEqualTo(LEGACY_ISSUE.equals(testData.getExpectedGenerator()));

    generateAndVerifySectionsContent(idToGenerator.get(testData.getExpectedGenerator()));
  }

  private void generateAndVerifySectionsContent(RuleDescriptionSectionsGenerator advancedRuleDescriptionSectionsGenerator) {
    assertThat(advancedRuleDescriptionSectionsGenerator.generateSections(rule))
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrderElementsOf(testData.getExpectedRuleDescriptionSectionsDto());
  }

}
