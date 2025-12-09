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
package org.sonar.server.authentication;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.api.server.authentication.IdentityProvider;

public class IdentityProviderRepository {
  private static final Predicate<IdentityProvider> IS_ENABLED_FILTER = IdentityProvider::isEnabled;
  private static final Function<IdentityProvider, String> TO_NAME = IdentityProvider::getName;

  protected final Map<String, IdentityProvider> providersByKey = new HashMap<>();

  public IdentityProviderRepository(@Nullable List<IdentityProvider> identityProviders) {
    Optional.ofNullable(identityProviders)
      .ifPresent(list -> list.forEach(i -> providersByKey.put(i.getKey(), i)));
  }

  public IdentityProvider getEnabledByKey(String key) {
    IdentityProvider identityProvider = providersByKey.get(key);
    if (identityProvider != null && IS_ENABLED_FILTER.test(identityProvider)) {
      return identityProvider;
    }
    throw new IllegalArgumentException(String.format("Identity provider %s does not exist or is not enabled", key));
  }

  public List<IdentityProvider> getAllEnabledAndSorted() {
    return providersByKey.values().stream()
      .filter(IS_ENABLED_FILTER)
      .sorted(Comparator.comparing(TO_NAME))
      .toList();
  }

}
