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
package org.sonar.core.platform;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.platform.EditionProvider.Edition.COMMUNITY;

public class PlatformEditionProvider implements EditionProvider {
  private static final EditionProvider[] NO_OTHER_PROVIDERS = new EditionProvider[0];
  private final EditionProvider[] otherEditionProviders;

  public PlatformEditionProvider() {
    this(NO_OTHER_PROVIDERS);
  }

  public PlatformEditionProvider(EditionProvider[] otherEditionProviders) {
    this.otherEditionProviders = otherEditionProviders;
  }

  @Override
  public Optional<Edition> get() {
    checkState(otherEditionProviders.length <= 1, "There can't be more than 1 other EditionProvider");
    if (otherEditionProviders.length == 1) {
      return otherEditionProviders[0].get();
    }
    return Optional.of(COMMUNITY);
  }
}
