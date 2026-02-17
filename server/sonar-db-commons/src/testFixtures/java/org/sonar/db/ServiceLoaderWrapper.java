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
package org.sonar.db;

import java.util.ServiceLoader;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a mockable wrapper around the ServiceLoader class.
 * It doesn't work well to mock ServiceLoader, in part because
 * the actual test infrastructure seems to use ServiceLoader.
 * It's intentional that the class instance basically does nothing,
 * because we're going to mock it and don't want anything
 * in here that needs testing.
 *
 * <p>When multiple implementations of a service exist on the classpath,
 * you can select one by setting a system property:
 * {@code -D<service-interface-name>.provider=<fully-qualified-provider-class-name>}
 * </p>
 *
 * <p>Example:
 * {@code -Dorg.sonar.db.TestDbProvider.provider=org.sonar.db.ServerTestDbProvider}
 * </p>
 */
class ServiceLoaderWrapper<T> {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceLoaderWrapper.class);

  private final Class<T> service;

  public ServiceLoaderWrapper(Class<T> service) {
    this.service = service;
  }

  static <T> T loadSingletonService(Class<T> service) {
    var loader = new ServiceLoaderWrapper<>(service).load();
    var services = loader.stream().toList();

    // Check for filtering system property
    String propertyName = service.getName() + ".provider";
    String preferredProvider = System.getProperty(propertyName);

    if (preferredProvider != null && !preferredProvider.isEmpty()) {
      LOG.info("Filtering {} providers to: {}", service.getName(), preferredProvider);

      // Filter to only the specified provider
      services = services.stream()
        .filter(provider -> provider.type().getName().equals(preferredProvider))
        .toList();

      if (services.isEmpty()) {
        var message = "Specified provider " + preferredProvider + " for " + service + " not found on classpath";
        LOG.error(message);
        throw new IllegalStateException(message);
      }
    }

    if (services.isEmpty()) {
      var message = "No implementation of " + service + " found on classpath";
      // the error gets wrapped in kind of an obscure way that's hard to read, so let's also log it
      LOG.error(message);
      throw new IllegalStateException(message);
    } else if (services.size() > 1) {
      var providers = services.stream()
        .map(p -> p.type().getName())
        .collect(Collectors.joining(", "));
      var message = "Multiple implementations of " + service + " found on classpath: [" + providers + "]. " +
        "Set system property '" + propertyName + "=<provider-class-name>' to select one.";
      LOG.error(message);
      throw new IllegalStateException(message);
    } else {
      T instance = services.get(0).get();
      LOG.info("Loaded {} provider: {}", service.getName(), instance.getClass().getName());
      return instance;
    }
  }

  public ServiceLoader<T> load() {
    return ServiceLoader.load(service);
  }
}
