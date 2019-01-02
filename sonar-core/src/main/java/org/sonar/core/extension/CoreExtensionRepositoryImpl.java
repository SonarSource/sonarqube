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

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CoreExtensionRepositoryImpl implements CoreExtensionRepository {
  private Set<CoreExtension> coreExtensions = null;
  private Set<CoreExtension> installedCoreExtensions = null;

  @Override
  public void setLoadedCoreExtensions(Set<CoreExtension> coreExtensions) {
    checkState(this.coreExtensions == null, "Repository has already been initialized");

    this.coreExtensions = ImmutableSet.copyOf(coreExtensions);
    this.installedCoreExtensions = new HashSet<>(coreExtensions.size());
  }

  @Override
  public Stream<CoreExtension> loadedCoreExtensions() {
    checkInitialized();

    return coreExtensions.stream();
  }

  @Override
  public void installed(CoreExtension coreExtension) {
    checkInitialized();
    requireNonNull(coreExtension, "coreExtension can't be null");
    checkArgument(coreExtensions.contains(coreExtension), "Specified CoreExtension has not been loaded first");

    this.installedCoreExtensions.add(coreExtension);
  }

  @Override
  public boolean isInstalled(String coreExtensionName) {
    checkInitialized();

    return installedCoreExtensions.stream()
      .anyMatch(t -> coreExtensionName.equals(t.getName()));
  }

  private void checkInitialized() {
    checkState(coreExtensions != null, "Repository has not been initialized yet");
  }
}
