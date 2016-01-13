/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.batch.sensor.internal;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputFile;

import javax.annotation.Nullable;

public abstract class DefaultStorable {

  protected transient final SensorStorage storage;
  private transient boolean saved = false;

  public DefaultStorable() {
    this.storage = null;
  }

  public DefaultStorable(@Nullable SensorStorage storage) {
    this.storage = storage;
  }

  public final void save() {
    Preconditions.checkNotNull(this.storage, "No persister on this object");
    Preconditions.checkState(!saved, "This object was already saved");
    doSave();
    this.saved = true;
  }

  protected abstract void doSave();

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  protected void validateLineArgument(InputFile inputFile, int line, String label) {
    Preconditions.checkArgument(line > 0 && line <= inputFile.lines(), "Invalid " + label + ": " + line + ". File " + inputFile + " has " + inputFile.lines()
      + " lines.");
  }

}
