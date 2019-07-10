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
package org.sonar.api.batch.sensor.internal;

import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkState;

public abstract class DefaultStorable {

  protected final SensorStorage storage;
  private boolean saved = false;

  public DefaultStorable() {
    this.storage = null;
  }

  public DefaultStorable(@Nullable SensorStorage storage) {
    this.storage = storage;
  }

  public final void save() {
    requireNonNull(this.storage, "No persister on this object");
    checkState(!saved, "This object was already saved");
    doSave();
    this.saved = true;
  }

  protected abstract void doSave();

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
