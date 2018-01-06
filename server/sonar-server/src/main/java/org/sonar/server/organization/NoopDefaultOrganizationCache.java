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
package org.sonar.server.organization;

/**
 * This implementation of {@link DefaultOrganizationCache} has no effect. It is used when SQ is running in safe mode
 * (ie. when we are expecting a DB upgrade and we can't assume the tables storing default organization information are
 * available).
 */
public class NoopDefaultOrganizationCache implements DefaultOrganizationCache {
  @Override
  public void load() {
    // do nothing
  }

  @Override
  public void unload() {
    // do nothing
  }
}
