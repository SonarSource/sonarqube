/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;

import static java.util.Objects.requireNonNull;

public class DefaultIssueLocation implements NewIssueLocation, IssueLocation {

  private InputComponent component;
  private TextRange textRange;
  private String message;

  @Override
  public DefaultIssueLocation on(InputComponent component) {
    Preconditions.checkArgument(component != null, "Component can't be null");
    Preconditions.checkState(this.component == null, "on() already called");
    this.component = component;
    return this;
  }

  @Override
  public DefaultIssueLocation at(TextRange location) {
    Preconditions.checkState(this.component != null, "at() should be called after on()");
    Preconditions.checkState(this.component.isFile(), "at() should be called only for an InputFile.");
    DefaultInputFile file = (DefaultInputFile) this.component;
    file.validate(location);
    this.textRange = location;
    return this;
  }

  @Override
  public DefaultIssueLocation message(String message) {
    requireNonNull(message, "Message can't be null");
    this.message = StringUtils.abbreviate(StringUtils.trim(message), MESSAGE_MAX_SIZE);
    return this;
  }

  @Override
  public InputComponent inputComponent() {
    return this.component;
  }

  @Override
  public TextRange textRange() {
    return textRange;
  }

  @Override
  public String message() {
    return this.message;
  }

}
