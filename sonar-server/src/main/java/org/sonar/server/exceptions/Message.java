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
package org.sonar.server.exceptions;

import com.google.common.base.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

public class Message {

  public static enum Level {
    INFO, WARNING, ERROR
  }

  private final String code;
  private String label;
  private Object[] l10nParams = new Object[0];
  private Level level;
  private Map<String, Object> params;

  private Message(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
@CheckForNull
  public String getLabel() {
    return label;
  }

  public Message setLabel(String s) {
    this.label = s;
    return this;
  }

  public Object[] getL10nParams() {
    return l10nParams;
  }

  public Message setL10nParams(Object[] l10nParams) {
    this.l10nParams = l10nParams;
    return this;
  }

  public Level getLevel() {
    return level;
  }

  public Message setLevel(Level level) {
    this.level = level;
    return this;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public Message setParams(Map<String, Object> params) {
    this.params = params;
    return this;
  }

  public Message setParam(String key, @Nullable Object value) {
    this.params.put(key, value);
    return this;
  }

  public static Message newError(String code) {
    return new Message(code).setLevel(Level.ERROR);
  }

  public static Message newWarning(String code) {
    return new Message(code).setLevel(Level.WARNING);
  }

  public static Message newInfo(String code) {
    return new Message(code).setLevel(Level.INFO);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("code", code)
      .add("label", label)
      .add("l10nParams", l10nParams)
      .add("level", level)
      .add("params", params)
      .toString();
  }
}
