/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * An interface exposing with the same interface as {@link java.nio.file.Paths} which classes can get injected
 * with instead of statically binding to {@link java.nio.file.Paths} class.
 * <p>
 * This interface can be used to improve testability of classes.
 */
@ServerSide
@ComputeEngineSide
public interface Paths2 {
  /**
   * @see java.nio.file.Paths#get(String, String...)  
   */
  Path get(String first, String... more);

  /**
   * @see java.nio.file.Paths#get(URI) 
   */
  Path get(URI uri);

  boolean exists(String first, String... more);
}
