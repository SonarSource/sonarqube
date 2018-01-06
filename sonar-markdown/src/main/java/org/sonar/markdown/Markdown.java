/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.channel.ChannelDispatcher;
import org.sonar.channel.CodeReader;

/**
 * Entry point of the Markdown library
 */
public final class Markdown {

  private ChannelDispatcher<MarkdownOutput> dispatcher;

  private Markdown() {
    dispatcher = ChannelDispatcher.builder()
      .addChannel(new HtmlLinkChannel())
      .addChannel(new HtmlUrlChannel())
      .addChannel(new HtmlEndOfLineChannel())
      .addChannel(new HtmlEmphasisChannel())
      .addChannel(new HtmlListChannel())
      .addChannel(new HtmlBlockquoteChannel())
      .addChannel(new HtmlHeadingChannel())
      .addChannel(new HtmlCodeChannel())
      .addChannel(new HtmlMultilineCodeChannel())
      .addChannel(new IdentifierAndNumberChannel())
      .addChannel(new BlackholeChannel())
      .build();
  }

  private String convert(String input) {
    CodeReader reader = new CodeReader(input);
    MarkdownOutput output = new MarkdownOutput();
    dispatcher.consume(reader, output);
    return output.toString();
  }

  public static String convertToHtml(String input) {
    return new Markdown().convert(StringEscapeUtils.escapeHtml(input));
  }
}
