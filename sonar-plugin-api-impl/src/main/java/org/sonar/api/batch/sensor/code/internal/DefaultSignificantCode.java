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
package org.sonar.api.batch.sensor.code.internal;

import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.utils.Preconditions;

public class DefaultSignificantCode extends DefaultStorable implements NewSignificantCode {
  private SortedMap<Integer, TextRange> significantCodePerLine = new TreeMap<>();
  private InputFile inputFile;

  public DefaultSignificantCode() {
    super();
  }

  public DefaultSignificantCode(@Nullable SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultSignificantCode onFile(InputFile inputFile) {
    this.inputFile = inputFile;
    return this;
  }

  @Override
  public DefaultSignificantCode addRange(TextRange range) {
    Preconditions.checkState(this.inputFile != null, "addRange() should be called after on()");

    int line = range.start().line();

    Preconditions.checkArgument(line == range.end().line(), "Ranges of significant code must be located in a single line");
    Preconditions.checkState(!significantCodePerLine.containsKey(line), "Significant code was already reported for line '%s'. Can only report once per line.", line);

    significantCodePerLine.put(line, range);
    return this;
  }

  @Override
  protected void doSave() {
    Preconditions.checkState(inputFile != null, "Call onFile() first");
    storage.store(this);
  }

  public InputFile inputFile() {
    return inputFile;
  }

  public SortedMap<Integer, TextRange> significantCodePerLine() {
    return significantCodePerLine;
  }
}
