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
package org.sonar.colorizer;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.sonar.channel.CodeReader;

public class HtmlDecorator extends Tokenizer {

  private static final String CSS_PATH = "/sonar-colorizer.css";

  private HtmlOptions options;
  private int lineId;

  private static final int LF = (int) '\n';
  private static final int CR = (int) '\r';

  public HtmlDecorator(HtmlOptions options) {
    this.options = options;
    this.lineId = options.getFirstLineId();
  }

  public String getTagBeginOfFile() {
    StringBuilder sb = new StringBuilder();
    if (options.isGenerateHtmlHeader()) {
      sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
          + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head><style type=\"text/css\">");
      sb.append(getCss());
      sb.append("</style></head><body>");
    }
    sb.append("<table class=\"code\" id=\"");
    if (options.getTableId() != null) {
      sb.append(options.getTableId());
    }
    sb.append("\"><tbody>");
    sb.append("<tr id=\"" + lineId++ + "\"><td><pre>");
    return sb.toString();
  }

  public String getTagEndOfFile() {
    StringBuilder sb = new StringBuilder();
    sb.append("</pre></td></tr></tbody></table>");
    if (options.isGenerateHtmlHeader()) {
      sb.append("</body></html>");
    }
    return sb.toString();
  }

  public String getTagBefore() {
    return "<tr id=\"" + lineId++ + "\"><td><pre>";
  }

  public String getTagAfter() {
    return "</pre></td></tr>";
  }

  @Override
  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    int lineNumber = code.getLinePosition();
    if (code.peek() == LF || code.peek() == CR) {
      code.pop();
      if (lineNumber != code.getLinePosition()) {
        codeBuilder.appendWithoutTransforming(getTagAfter());
        codeBuilder.appendWithoutTransforming(getTagBefore());
      }
      return true;
    }
    return false;
  }

  public static String getCss() {
    InputStream input = null;
    try {
      input = HtmlRenderer.class.getResourceAsStream(CSS_PATH);
      return IOUtils.toString(input);

    } catch (IOException e) {
      throw new SynhtaxHighlightingException("Sonar Colorizer CSS file not found: " + CSS_PATH, e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }
}
