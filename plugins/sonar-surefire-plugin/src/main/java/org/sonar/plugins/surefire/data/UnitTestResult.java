/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.surefire.data;

import org.apache.commons.lang.StringEscapeUtils;

public final class UnitTestResult {
  public final static String STATUS_OK = "ok";
  public final static String STATUS_ERROR = "error";
  public final static String STATUS_FAILURE = "failure";
  public final static String STATUS_SKIPPED = "skipped";

  private String name, status, stackTrace, message;
  private long durationMilliseconds = 0L;

  public String getName() {
    return name;
  }

  public UnitTestResult setName(String name) {
    this.name = name;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public UnitTestResult setStatus(String status) {
    this.status = status;
    return this;
  }

  public UnitTestResult setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public UnitTestResult setMessage(String message) {
    this.message = message;
    return this;
  }

  public long getDurationMilliseconds() {
    return durationMilliseconds;
  }

  public UnitTestResult setDurationMilliseconds(long l) {
    this.durationMilliseconds = l;
    return this;
  }

  public boolean isErrorOrFailure() {
    return STATUS_ERROR.equals(status) || STATUS_FAILURE.equals(status);
  }

  public boolean isError() {
    return STATUS_ERROR.equals(status);
  }

  public String toXml() {
    StringBuilder sb = new StringBuilder();
    return appendXml(sb).toString();
  }
  public StringBuilder appendXml(StringBuilder sb) {
    sb
        .append("<testcase status=\"")
        .append(status)
        .append("\" time=\"")
        .append(durationMilliseconds)
        .append("\" name=\"")
        .append(StringEscapeUtils.escapeXml(name))
        .append("\"");

    if (isErrorOrFailure()) {
      sb
          .append(">")
          .append(isError() ? "<error message=\"" : "<failure message=\"")
          .append(StringEscapeUtils.escapeXml(message))
          .append("\">")
          .append("<![CDATA[")
          .append(StringEscapeUtils.escapeXml(stackTrace))
          .append("]]>")
          .append(isError() ? "</error>" : "</failure>")
          .append("</testcase>");
    } else {
      sb.append("/>");
    }
    return sb;
  }

}
