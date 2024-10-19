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
package org.sonar.markdown;

import org.sonar.channel.RegexChannel;

/**
 * Markdown will wrap any URL with an HTML {@literal <a href="URL">} tag.
 * 
 */
class HtmlUrlChannel extends RegexChannel<MarkdownOutput> {

  public HtmlUrlChannel() {
    super("https?://[\\w\\d:#@%/;$()~_?\\+-=\\.&]++");
  }

  @Override
  protected void consume(CharSequence token, MarkdownOutput output) {
    String url = token.toString();
    boolean isRelativeUrl = !url.contains("://");
    output.append("<a href=\"");
    output.append(url);
    output.append(isRelativeUrl ? "\">" : "\" target=\"_blank\" rel=\"noopener noreferrer\">");
    output.append(url);
    output.append("</a>");
  }
}
