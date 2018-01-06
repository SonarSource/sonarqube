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
package org.sonar.server.authentication;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.server.authentication.IdentityProvider;

import static com.google.common.collect.FluentIterable.from;

public class IdentityProviderRepository {

  protected final Map<String, IdentityProvider> providersByKey = new HashMap<>();

  public IdentityProviderRepository(List<IdentityProvider> identityProviders) {
    this.providersByKey.putAll(FluentIterable.from(identityProviders).uniqueIndex(ToKey.INSTANCE));
  }

  /**
   * Used by pico when no identity provider available
   */
  public IdentityProviderRepository() {
    this.providersByKey.clear();
  }

  public IdentityProvider getEnabledByKey(String key) {
    IdentityProvider identityProvider = providersByKey.get(key);
    if (identityProvider != null && IsEnabledFilter.INSTANCE.apply(identityProvider)) {
      return identityProvider;
    }
    throw new IllegalArgumentException(String.format("Identity provider %s does not exist or is not enabled", key));
  }

  public List<IdentityProvider> getAllEnabledAndSorted() {
    return from(providersByKey.values())
      .filter(IsEnabledFilter.INSTANCE)
      .toSortedList(
        Ordering.natural().onResultOf(ToName.INSTANCE)
      );
  }

  private enum IsEnabledFilter implements Predicate<IdentityProvider> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull IdentityProvider input) {
      return input.isEnabled();
    }
  }

  private enum ToKey implements Function<IdentityProvider, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull IdentityProvider input) {
      return input.getKey();
    }
  }

  private enum ToName implements Function<IdentityProvider, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull IdentityProvider input) {
      return input.getName();
    }
  }
}
