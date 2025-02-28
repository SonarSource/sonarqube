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

import org.junit.Test;
import org.sonar.core.rule.RuleType;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleDto.Format.HTML;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;

public class RuleDescriptionFormatterTest {

  private static final RuleDescriptionSectionDto HTML_SECTION = createDefaultRuleDescriptionSection("uuid", "<span class=\"example\">*md* ``description``</span>");
  private static final RuleDescriptionSectionDto MARKDOWN_SECTION = createDefaultRuleDescriptionSection("uuid", "*md* ``description``");
  private static final RuleDescriptionFormatter ruleDescriptionFormatter = new RuleDescriptionFormatter();

  @Test
  public void getMarkdownDescriptionAsHtml() {
    RuleDto rule = new RuleDto().setDescriptionFormat(MARKDOWN).addRuleDescriptionSectionDto(MARKDOWN_SECTION).setType(RuleType.BUG);
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
  public void getDescriptionAsHtml_ignoresAdvancedSections() {
    var section1 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>");
    var section2 = createRuleDescriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY, "<div>This is not a problem</div>");
    var defaultRuleDescriptionSection = createDefaultRuleDescriptionSection("uuid_432", "default description");
    RuleDto rule = new RuleDto().setDescriptionFormat(RuleDto.Format.HTML)
      .setType(RuleType.SECURITY_HOTSPOT)
      .addRuleDescriptionSectionDto(section1)
      .addRuleDescriptionSectionDto(section2)
      .addRuleDescriptionSectionDto(defaultRuleDescriptionSection);
    String html = ruleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html).isEqualTo(defaultRuleDescriptionSection.getContent());
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

  @Test
  public void toHtmlWithNullFormat() {
    RuleDescriptionSectionDto section = createRuleDescriptionSection(DEFAULT_KEY, "whatever");
    String result = ruleDescriptionFormatter.toHtml(null, section);
    assertThat(result).isEqualTo(section.getContent());
  }

  @Test
  public void toHtmlWithMarkdownFormat() {
    String result = ruleDescriptionFormatter.toHtml(MARKDOWN, MARKDOWN_SECTION);
    assertThat(result).isEqualTo("<strong>md</strong> <code>description</code>");
  }

  @Test
  public void toHtmlWithHtmlFormat() {
    String result = ruleDescriptionFormatter.toHtml(HTML, HTML_SECTION);
    assertThat(result).isEqualTo(HTML_SECTION.getContent());
  }

  private static RuleDescriptionSectionDto createRuleDescriptionSection(String key, String content) {
    return RuleDescriptionSectionDto.builder().key(key).content(content).build();
  }
}
