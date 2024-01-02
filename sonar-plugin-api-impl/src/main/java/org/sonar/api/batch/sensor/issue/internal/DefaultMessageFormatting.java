/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.sensor.issue.internal;

import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewMessageFormatting;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class DefaultMessageFormatting implements MessageFormatting, NewMessageFormatting {
  private int start;
  private int end;
  private Type type;

  @Override
  public int start() {
    return start;
  }

  @Override
  public int end() {
    return end;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public DefaultMessageFormatting start(int start) {
    this.start = start;
    return this;
  }

  @Override
  public DefaultMessageFormatting end(int end) {
    this.end = end;
    return this;
  }

  @Override
  public DefaultMessageFormatting type(Type type) {
    this.type = type;
    return this;
  }

  public void validate(String message) {
    checkArgument(this.type() != null, "Message formatting type can't be null");
    checkArgument(this.start() >= 0, "Message formatting start must be greater or equals to 0");
    checkArgument(this.end() <= message.length(), "Message formatting end must be lesser or equal than message size");
    checkArgument(this.end() > this.start(), "Message formatting end must be greater than start");
  }
}
