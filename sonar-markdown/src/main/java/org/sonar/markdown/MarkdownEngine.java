/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.markdown;

import java.util.ArrayList;
import java.util.List;

import org.sonar.channel.Channel;
import org.sonar.channel.ChannelDispatcher;
import org.sonar.channel.CodeReader;

/**
 * Entry point of the Markdown library
 */
public class MarkdownEngine {

  private MarkdownOutput output;
  private ChannelDispatcher<MarkdownOutput> dispatcher;

  private MarkdownEngine() {
    output = new MarkdownOutput();
    List<Channel> markdownChannels = new ArrayList<Channel>();
    markdownChannels.add(new HtmlUrlChannel());
    markdownChannels.add(new HtmlEndOfLineChannel());
    markdownChannels.add(new HtmlEmphasisChannel());
    markdownChannels.add(new HtmlCodeChannel());
    markdownChannels.add(new IdentifierAndNumberChannel());
    markdownChannels.add(new BlackholeChannel());
    dispatcher = new ChannelDispatcher<MarkdownOutput>(markdownChannels);
  }

  public static String convertToHtml(String input) {
    MarkdownEngine engine = new MarkdownEngine();
    engine.dispatcher.consume(new CodeReader(input), engine.output);
    return engine.output.toString();
  }
}
