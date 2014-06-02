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
package org.sonar.core.log;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.sonar.core.persistence.Dto;
import org.sonar.core.log.db.LogKey;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;

/**
 * @since 4.4
 */
public class LogDto extends Dto<LogKey> {

  public static enum Type {
    CHANGE, LOG
  }

  public static enum Status {
    OK, FAIL
  }

  private Date time;
  private Type type;
  private Status status;
  private Long executionTime;
  private String author;
  private String data;

  private LogDto(Date time, Type type) {
    this.time = time;
    this.type = type;
  }

  @Override
  public LogKey getKey() {
    return LogKey.of(time, type, author);
  }

  public Date getTime() {
    return time;
  }

  public LogDto setTime(Date time) {
    this.time = time;
    return this;
  }

  public Type getType() {
    return type;
  }

  public LogDto setType(Type type) {
    this.type = type;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public LogDto setStatus(Status status) {
    this.status = status;
    return this;
  }

  public Long getExecutionTime() {
    return executionTime;
  }

  public LogDto setExecutionTime(Long executionTime) {
    this.executionTime = executionTime;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public LogDto setAuthor(String author) {
    this.author = author;
    return this;
  }

  public Map getPayload() {
    try {
      byte[] bytes = this.data.getBytes();
      ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(bytes));
      Map payload = (Map) ois.readObject();
      ois.close();
      return payload;
    } catch (Exception e) {
      throw new IllegalStateException("Could not read payload from DB.", e);
    }
  }

  public LogDto setPayload(Map payload) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(payload);
      oos.close();
      this.data = new String(baos.toByteArray());
    } catch (Exception e) {
      throw new IllegalStateException("Could not write payload from DB.", e);
    }
    return this;
  }

  public LogDto changeLog() {
    return new LogDto(new Date(), Type.CHANGE);
  }
}
