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
package org.sonar.core.extension;

import java.util.Set;
import java.util.stream.Stream;

public interface CoreExtensionRepository {
  /**
   * Register the loaded Core Extensions in the repository.
   *
   * @throws IllegalStateException if the setCoreExtensionNames has already been called
   */
  void setLoadedCoreExtensions(Set<CoreExtension> coreExtensions);

  /**
   * @return a {@link Stream} of the loaded Core Extensions (if any).
   *
   * @see CoreExtension#getName()
   * @throws IllegalStateException if {@link #setLoadedCoreExtensions(Set)} has not been called yet
   */
  Stream<CoreExtension> loadedCoreExtensions();

  /**
   * Register that the specified Core Extension has been installed.
   *
   * @throws IllegalArgumentException if the specified {@link CoreExtension} has not been loaded prior to this call
   *         ({@link #setLoadedCoreExtensions(Set)}
   * @throws IllegalStateException if {@link #setLoadedCoreExtensions(Set)} has not been called yet
   */
  void installed(CoreExtension coreExtension);

  /**
   * Tells whether the repository knows of Core Extension with this exact name.
   *
   * @see CoreExtension#getName()
   * @throws IllegalStateException if {@link #setLoadedCoreExtensions(Set)} has not been called yet
   */
  boolean isInstalled(String coreExtensionName);
}
