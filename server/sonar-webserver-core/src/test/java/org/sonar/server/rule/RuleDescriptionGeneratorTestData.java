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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleDescriptionSection;
import org.sonar.db.rule.RuleDescriptionSectionDto;

class RuleDescriptionGeneratorTestData {
  enum RuleDescriptionSectionGeneratorIdentifier {
    LEGACY_ISSUE, LEGACY_HOTSPOT, ADVANCED_RULE;
  }

  private final RuleType ruleType;
  private final String htmlDescription;

  private final String markdownDescription;
  private final List<RuleDescriptionSection> ruleDescriptionSections;
  private final RuleDescriptionSectionGeneratorIdentifier expectedGenerator;
  private final Set<RuleDescriptionSectionDto> expectedRuleDescriptionSectionsDto;

  private RuleDescriptionGeneratorTestData(RuleType ruleType, @Nullable String htmlDescription,@Nullable String markdownDescription, List<RuleDescriptionSection> ruleDescriptionSections,
    RuleDescriptionSectionGeneratorIdentifier expectedGenerator, Set<RuleDescriptionSectionDto> expectedRuleDescriptionSectionsDto) {
    this.ruleType = ruleType;
    this.htmlDescription = htmlDescription;
    this.markdownDescription = markdownDescription;
    this.ruleDescriptionSections = ruleDescriptionSections;
    this.expectedGenerator = expectedGenerator;
    this.expectedRuleDescriptionSectionsDto = expectedRuleDescriptionSectionsDto;
  }

  public RuleType getRuleType() {
    return ruleType;
  }

  String getHtmlDescription() {
    return htmlDescription;
  }

  String getMarkdownDescription() {
    return markdownDescription;
  }

  List<RuleDescriptionSection> getRuleDescriptionSections() {
    return ruleDescriptionSections;
  }

  RuleDescriptionSectionGeneratorIdentifier getExpectedGenerator() {
    return expectedGenerator;
  }

  public Set<RuleDescriptionSectionDto> getExpectedRuleDescriptionSectionsDto() {
    return expectedRuleDescriptionSectionsDto;
  }

  static RuleDescriptionGeneratorTestDataBuilder aRuleOfType(RuleType ruleType) {
    return new RuleDescriptionGeneratorTestDataBuilder(ruleType);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ")
      .add(ruleType.name())
      .add(htmlDescription == null ? "html present" : "html absent")
      .add(markdownDescription == null ? "md present" : "md absent")
      .add(String.valueOf(ruleDescriptionSections.size()))
      .add("generator=" + expectedGenerator)
      .toString();
  }

  public static final class RuleDescriptionGeneratorTestDataBuilder {
    private final RuleType ruleType;
    private String htmlDescription;
    private String markdownDescription;
    private List<RuleDescriptionSection> ruleDescriptionSections = new ArrayList<>();
    private Set<RuleDescriptionSectionDto> expectedRuleDescriptionSectionsDto = new HashSet<>();
    private RuleDescriptionSectionGeneratorIdentifier expectedGenerator;

    private RuleDescriptionGeneratorTestDataBuilder(RuleType ruleType) {
      this.ruleType = ruleType;
    }

    RuleDescriptionGeneratorTestDataBuilder html(@Nullable String htmlDescription) {
      this.htmlDescription = htmlDescription;
      return this;
    }

    RuleDescriptionGeneratorTestDataBuilder md(@Nullable String markdownDescription) {
      this.markdownDescription = markdownDescription;
      return this;
    }

    RuleDescriptionGeneratorTestDataBuilder addSection(RuleDescriptionSection ruleDescriptionSection) {
      this.ruleDescriptionSections.add(ruleDescriptionSection);
      return this;
    }

    RuleDescriptionGeneratorTestDataBuilder expectedGenerator(RuleDescriptionSectionGeneratorIdentifier generatorToUse) {
      this.expectedGenerator = generatorToUse;
      return this;
    }

    RuleDescriptionGeneratorTestDataBuilder addExpectedSection(RuleDescriptionSectionDto sectionDto) {
      this.expectedRuleDescriptionSectionsDto.add(sectionDto);
      return this;
    }

    RuleDescriptionGeneratorTestData build() {
      return new RuleDescriptionGeneratorTestData(ruleType, htmlDescription, markdownDescription, ruleDescriptionSections, expectedGenerator, expectedRuleDescriptionSectionsDto);
    }
  }
}
