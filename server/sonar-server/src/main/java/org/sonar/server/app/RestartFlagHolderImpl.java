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
package org.sonar.server.app;

import java.util.concurrent.atomic.AtomicBoolean;

public class RestartFlagHolderImpl implements RestartFlagHolder {
  private final AtomicBoolean restarting = new AtomicBoolean(false);

  @Override
  public boolean isRestarting() {
    return restarting.get();
  }

  @Override
  public void set() {
    restarting.set(true);
  }

  @Override
  public void unset() {
    restarting.set(false);
  }
}
