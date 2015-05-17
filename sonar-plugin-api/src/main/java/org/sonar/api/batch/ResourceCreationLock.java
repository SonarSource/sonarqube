/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch;

import org.sonar.api.BatchSide;

/**
 * This lock is used to ensure that Sonar resources (files, packages, directories) are not created by buggy plugins
 * when saving measures/violations on unknown resources.
 * 
 * @since 2.3
 * @deprecated since 4.2. File system is immutable and does not require this class anymore.
 */
@Deprecated
@BatchSide
public interface ResourceCreationLock {

  /**
   * Forbids the creation of resources when saving violations and measures. By default it's unlocked, so only warnings
   * are logged. When locked, then an exception is thrown.
   */
  void lock();

}
