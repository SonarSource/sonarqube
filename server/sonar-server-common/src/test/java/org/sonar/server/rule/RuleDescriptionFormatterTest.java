/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Optional;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.INTRODUCTION_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;

public class RuleDescriptionFormatterTest {

  private static final RuleDescriptionSectionDto HTML_SECTION = createDefaultRuleDescriptionSection("uuid", "<span class=\"example\">*md* ``description``</span>");
  private static final RuleDescriptionSectionDto MARKDOWN_SECTION = createDefaultRuleDescriptionSection("uuid", "*md* ``description``");
  private static final RuleDescriptionFormatter ruleDescriptionFormatter = new RuleDescriptionFormatter();

  @Test
  public void getMarkdownDescriptionAsHtml() {
    RuleDto rule = new RuleDto().setDescriptionFormat(RuleDto.Format.MARKDOWN).addRuleDescriptionSectionDto(MARKDOWN_SECTION).setType(RuleType.BUG);
    String html = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html).isEqualTo("<strong>md</strong> <code>description</code>");
  }

  @Test
  public void getHtmlDescriptionAsIs() {
    RuleDto rule = new RuleDto().setDescriptionFormat(RuleDto.Format.HTML).addRuleDescriptionSectionDto(HTML_SECTION).setType(RuleType.BUG);
    String html = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html).isEqualTo(HTML_SECTION.getContent());
  }

  @Test
  public void concatHtmlDescriptionSections() {
    var section1 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>");
    var section2 = createRuleDescriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY, "<div>This is not a problem</div>");
    var section3 = createRuleDescriptionSection(HOW_TO_FIX_SECTION_KEY, "<div>I don't want to fix</div>");
    var section4 = createRuleDescriptionSection(INTRODUCTION_SECTION_KEY, "<div>Introduction with no title</div>");
    var section5ctx1 = createRuleDescriptionSection(RESOURCES_SECTION_KEY, "<div>CTX_1</div>", "CTX_1");
    var section5ctx2 = createRuleDescriptionSection(RESOURCES_SECTION_KEY, "<div>CTX_2</div>", "CTX_2");
    RuleDto rule = new RuleDto().setDescriptionFormat(RuleDto.Format.HTML)
      .setType(RuleType.SECURITY_HOTSPOT)
      .addRuleDescriptionSectionDto(section1)
      .addRuleDescriptionSectionDto(section2)
      .addRuleDescriptionSectionDto(section3)
      .addRuleDescriptionSectionDto(section4)
      .addRuleDescriptionSectionDto(section5ctx2)
      .addRuleDescriptionSectionDto(section5ctx1);
    String html = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html)
      .isEqualTo(
        "<div>Introduction with no title</div><br/>"
          + "<h2>What is the risk?</h2>"
          + "<div>Root is Root</div><br/>"
          + "<h2>Assess the risk</h2>"
          + "<div>This is not a problem</div><br/>"
          + "<h2>How can you fix it?</h2>"
          + "<div>I don't want to fix</div><br/>"
          + "<div>CTX_1</div><br/>"
      );
  }

  @Test
  public void handleEmptyDescription() {
    RuleDto rule = new RuleDto().setDescriptionFormat(RuleDto.Format.HTML).setType(RuleType.BUG);
    String result = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(result).isNull();
  }

  @Test
  public void handleNullDescriptionFormat() {
    RuleDescriptionSectionDto sectionWithNullFormat = createDefaultRuleDescriptionSection("uuid", "whatever");
    RuleDto rule = new RuleDto().addRuleDescriptionSectionDto(sectionWithNullFormat).setType(RuleType.BUG);
    String result = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(result).isNull();
  }

  private static RuleDescriptionSectionDto createRuleDescriptionSection(String key, String content) {
    return createRuleDescriptionSection(key, content, null);
  }

  private static RuleDescriptionSectionDto createRuleDescriptionSection(String key, String content, @Nullable String contextKey) {
    RuleDescriptionSectionContextDto context = Optional.ofNullable(contextKey)
      .map(c -> RuleDescriptionSectionContextDto.of(contextKey, contextKey + RandomStringUtils.randomAlphanumeric(20)))
      .orElse(null);
    return RuleDescriptionSectionDto.builder().key(key).content(content).context(context).build();
  }
}
