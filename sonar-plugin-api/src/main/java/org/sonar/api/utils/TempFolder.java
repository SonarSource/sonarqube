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
package org.sonar.api.utils;

import java.io.File;
import javax.annotation.Nullable;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * Use this component to deal with temp files/folders. Root location of temp files/folders
 * depends on situation:
 * <ul>
 * <li>${SONAR_HOME}/temp on server side</li>
 * <li>${SONAR_HOME}/.sonartmp on scanner side</li>
 * </ul>
 * @since 4.0
 *
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public interface TempFolder {

  /**
   * Create a directory in temp folder with a random unique name.
   */
  File newDir();

  /**
   * Create a directory in temp folder using provided name.
   */
  File newDir(String name);

  File newFile();

  File newFile(@Nullable String prefix, @Nullable String suffix);

}
