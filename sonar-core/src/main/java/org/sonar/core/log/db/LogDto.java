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
package org.sonar.core.log.db;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.log.Log;
import org.sonar.core.log.Loggable;
import org.sonar.core.persistence.Dto;

/**
 * @since 4.4
 */
public final class LogDto extends Dto<LogKey> {

  private String message;
  private Log.Type type;
  private String author;

  private Long executionTime;

  private String data;

  protected LogDto(){
  }

  @Override
  public LogKey getKey() {
    return LogKey.of(this.getCreatedAt(), type, author);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }

  public Log.Type getType() {
    return type;
  }

  public LogDto setType(Log.Type type) {
    this.type = type;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public LogDto setAuthor(String author) {
    this.author = author;
    return this;
  }

  public Long getExecutionTime() {
    return executionTime;
  }

  public LogDto setExecutionTime(Long executionTime) {
    this.executionTime = executionTime;
    return this;
  }

  public String getData() {
    return data;
  }

  public LogDto setData(String data) {
    this.data = data;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public LogDto setMessage(String message) {
    this.message = message;
    return this;
  }

  public static LogDto createFor(String message) {
    return new LogDto()
      .setMessage(message);
  }

  public static LogDto createFor(Loggable loggable) {
    return new LogDto()
      .setData(KeyValueFormat.format(loggable.getDetails()))
      .setExecutionTime(loggable.getExecutionTime());
  }
}
