/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.batch.highlighting.SyntaxHighlightingDataBuilder;
import org.sonar.colorizer.HtmlCodeBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighlightingCodeBuilder extends HtmlCodeBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(HighlightingCodeBuilder.class);

  private static final char BOM = '\uFEFF';

  private SyntaxHighlightingDataBuilder highlightingBuilder = new SyntaxHighlightingDataBuilder();
  private int currentOffset = 0;
  private static final Pattern START_TAG_PATTERN = Pattern.compile("<span class=\"(.+)\">");
  private static final Pattern END_TAG_PATTERN = Pattern.compile("</span>");
  private int startOffset = -1;
  private String cssClass;

  @Override
  public Appendable append(CharSequence csq) {
    for (int i = 0; i < csq.length(); i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  @Override
  public Appendable append(char c) {
    if (c != BOM) {
      currentOffset++;
    }
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) {
    for (int i = start; i < end; i++) {
      append(csq.charAt(i));
    }
    return this;
  }

  public void appendWithoutTransforming(String htmlTag) {
    if (startOffset == -1) {
      Matcher startMatcher = START_TAG_PATTERN.matcher(htmlTag);
      if (startMatcher.matches()) {
        startOffset = currentOffset;
        cssClass = startMatcher.group(1);
      } else {
        LOG.warn("Expected to match highlighting start html tag but was: " + htmlTag);
      }
    } else {
      Matcher endMatcher = END_TAG_PATTERN.matcher(htmlTag);
      if (endMatcher.matches()) {
        highlightingBuilder.registerHighlightingRule(startOffset, currentOffset, TypeOfText.forCssClass(cssClass));
        startOffset = -1;
      } else {
        LOG.warn("Expected to match highlighting end html tag but was: " + htmlTag);
      }
    }
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException();
  }

  @Override
  public StringBuilder getColorizedCode() {
    throw new UnsupportedOperationException();
  }

  public SyntaxHighlightingData getHighlightingData() {
    return highlightingBuilder.build();
  }

}
