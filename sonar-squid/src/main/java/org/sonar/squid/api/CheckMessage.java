/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.squid.api;

import java.text.MessageFormat;
import java.util.Locale;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.check.Message;

public class CheckMessage implements Message {

  private Integer line;
  private SourceCode sourceCode;
  private CodeCheck codeCheck;
  private String defaultMessage;
  private Object[] messageArguments;

  public CheckMessage(CodeCheck rule, String message, Object... messageArguments) {
    this.codeCheck = rule;
    this.defaultMessage = message;
    this.messageArguments = messageArguments;
  }

  public void setSourceCode(SourceCode sourceCode) {
    this.sourceCode = sourceCode;
  }

  public SourceCode getSourceCode() {
    return sourceCode;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public Integer getLine() {
    return line;
  }

  public CodeCheck getChecker() {
    return codeCheck;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }

  public Object[] getMessageArguments() {
    return messageArguments;
  }

  public String getText(Locale locale) {
    return formatDefaultMessage();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("source", sourceCode).append("check", codeCheck).append("msg", defaultMessage)
        .append("line", line).toString();
  }

  public String formatDefaultMessage() {
    if (messageArguments.length == 0) {
      return defaultMessage;
    } else {
      return MessageFormat.format(defaultMessage, messageArguments);
    }
  }
}
