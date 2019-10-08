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
package org.sonar.server.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Paths2Impl implements Paths2 {
  private static final Paths2 INSTANCE = new Paths2Impl();

  private Paths2Impl() {
    // prevents instantiation and subclassing, use getInstance() instead
  }

  public static Paths2 getInstance() {
    return INSTANCE;
  }

  @Override
  public Path get(String first, String... more) {
    return Paths.get(first, more);
  }

  @Override
  public Path get(URI uri) {
    return Paths.get(uri);
  }
}
