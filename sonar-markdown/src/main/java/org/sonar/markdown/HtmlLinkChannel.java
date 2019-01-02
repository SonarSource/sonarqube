/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.markdown;

import org.sonar.channel.RegexChannel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown interprets text in brackets followed by text in parentheses to generate documented links.
 *
 * E.g., the input [See documentation](http://docs.sonarqube.org/display/SONAR) will produce
 * {@literal<a href="http://docs.sonarqube.org/display/SONAR">}See documentation{@literal</a>}
 */
class HtmlLinkChannel extends RegexChannel<MarkdownOutput> {

  private static final String LINK_REGEX = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)";

  private static final Pattern LINK_PATTERN = Pattern.compile(LINK_REGEX);

  public HtmlLinkChannel() {
    super(LINK_REGEX);
  }

  @Override
  protected void consume(CharSequence token, MarkdownOutput output) {
    Matcher matcher = LINK_PATTERN.matcher(token);
    // Initialize match groups
    matcher.matches();
    String content = matcher.group(1);
    String url = matcher.group(2);
    output.append("<a href=\"");
    output.append(url);
    output.append("\" target=\"_blank\">");
    output.append(content);
    output.append("</a>");
  }
}
