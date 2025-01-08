/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.util;

import java.io.File;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;

public interface ProjectExportDumpFS {

  /**
   * Create a File representing the exported dump of the specified {@link ProjectDescriptor}.
   * <p>
   * This method doesn't check nor enforce the File actually exist and/or is a regular file.
   * </p>
   */
  File exportDumpOf(ProjectDescriptor descriptor);
}
