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
package org.sonar.api.resources;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * The extension point to define a new language
 *
 * @since 1.10
 */
@ScannerSide
@ServerSide
@SonarLintSide
@ComputeEngineSide
@ExtensionPoint
public interface Language {

  /**
   * For example "java". Should not be more than 20 chars.
   */
  String getKey();

  /**
   * For example "Java"
   */
  String getName();

  /**
   * For example ["jav", "java"].
   * If empty, then all files in source directories are considered as sources.
   */
  String[] getFileSuffixes();

}
