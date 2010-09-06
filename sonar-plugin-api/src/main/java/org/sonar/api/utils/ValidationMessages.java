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
package org.sonar.api.utils;

import java.util.ArrayList;
import java.util.List;

public final class ValidationMessages {

  private List<Message> errors = new ArrayList<Message>();
  private List<Message> warnings = new ArrayList<Message>();
  private List<Message> infos = new ArrayList<Message>();

  ValidationMessages() {
  }

  public static ValidationMessages create() {
    return new ValidationMessages();
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
  public List<Message> getErrors() {
    return errors;
  }

  public List<Message> getWarnings() {
    return warnings;
  }

  public List<Message> getInfos() {
    return infos;
  }

  public ValidationMessages addError(String key, String label) {
    errors.add(new Message(key, label));
    return this;
  }

  public ValidationMessages addWarning(String key, String label) {
    warnings.add(new Message(key, label));
    return this;
  }

  public ValidationMessages addInfo(String key, String label) {
    infos.add(new Message(key, label));
    return this;
  }

  public static final class Message {
    private String key;
    private String label;

    private Message(String key, String label) {
      this.key = key;
      this.label = label;
    }

    public String getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }
  }
}
