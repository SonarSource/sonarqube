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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkState;

public class DisabledComponentsHolderImpl implements MutableDisabledComponentsHolder {

  private Collection<String> uuids;

  @Override
  public Collection<String> getUuids() {
    checkState(uuids != null, "UUIDs have not been set in repository");
    return uuids;
  }

  @Override
  public void setUuids(Collection<String> uuids) {
    checkState(this.uuids == null, "UUIDs have already been set in repository");
    this.uuids = uuids;
  }
}
