/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.error.internal;

import com.google.common.base.Preconditions;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static java.util.Objects.requireNonNull;

public class DefaultAnalysisError extends DefaultStorable implements NewAnalysisError, AnalysisError {
  private InputFile inputFile;
  private String message;
  private TextPointer location;

  public DefaultAnalysisError() {
    super(null);
  }

  public DefaultAnalysisError(SensorStorage storage) {
    super(storage);
  }

  @Override
  public InputFile inputFile() {
    return inputFile;
  }

  @Override
  public String message() {
    return message;
  }

  @Override
  public TextPointer location() {
    return location;
  }

  @Override
  public NewAnalysisError onFile(InputFile inputFile) {
    Preconditions.checkArgument(inputFile != null, "Cannot use a inputFile that is null");
    Preconditions.checkState(this.inputFile == null, "onFile() already called");
    this.inputFile = inputFile;
    return this;
  }

  @Override
  public NewAnalysisError message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public NewAnalysisError at(TextPointer location) {
    Preconditions.checkState(this.location == null, "at() already called");
    this.location = location;
    return this;
  }

  @Override
  protected void doSave() {
    requireNonNull(this.inputFile, "inputFile is mandatory on AnalysisError");
    storage.store(this);
  }

}
