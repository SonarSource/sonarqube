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

import org.junit.Test;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleDescriptionFormatterTest {

  @Test
  public void getMarkdownDescriptionAsHtml() {
    RuleDefinitionDto rule = new RuleDefinitionDto().setDescription("*md* ``description``").setDescriptionFormat(RuleDto.Format.MARKDOWN);
    String html = RuleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html).isEqualTo("<strong>md</strong> <code>description</code>");
  }

  @Test
  public void getHtmlDescriptionAsIs() {
    String description = "<span class=\"example\">*md* ``description``</span>";
    RuleDefinitionDto rule = new RuleDefinitionDto().setDescription(description).setDescriptionFormat(RuleDto.Format.HTML);
    String html = RuleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(html).isEqualTo(description);
  }

  @Test
  public void handleNullDescription() {
    RuleDefinitionDto rule = new RuleDefinitionDto().setDescription(null).setDescriptionFormat(RuleDto.Format.HTML);
    String result = RuleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(result).isNull();
  }

  @Test
  public void handleNullDescriptionFormat() {
    RuleDefinitionDto rule = new RuleDefinitionDto().setDescription("whatever").setDescriptionFormat(null);
    String result = RuleDescriptionFormatter.getDescriptionAsHtml(rule);
    assertThat(result).isNull();
  }
}
