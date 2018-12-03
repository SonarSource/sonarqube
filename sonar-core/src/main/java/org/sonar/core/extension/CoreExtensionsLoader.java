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
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Load {@link CoreExtension} and register them into the {@link CoreExtensionRepository}.
 */
public class CoreExtensionsLoader {
  private static final Logger LOG = Loggers.get(CoreExtensionsLoader.class);

  private final CoreExtensionRepository coreExtensionRepository;
  private final ServiceLoaderWrapper serviceLoaderWrapper;

  public CoreExtensionsLoader(CoreExtensionRepository coreExtensionRepository) {
    this(coreExtensionRepository, new ServiceLoaderWrapper());
  }

  CoreExtensionsLoader(CoreExtensionRepository coreExtensionRepository, ServiceLoaderWrapper serviceLoaderWrapper) {
    this.coreExtensionRepository = coreExtensionRepository;
    this.serviceLoaderWrapper = serviceLoaderWrapper;
  }

  public void load() {
    Set<CoreExtension> coreExtensions = serviceLoaderWrapper.load(getClass().getClassLoader());
    ensureNoDuplicateName(coreExtensions);

    coreExtensionRepository.setLoadedCoreExtensions(coreExtensions);
    if (!coreExtensions.isEmpty()) {
      LOG.info("Loaded core extensions: {}", coreExtensions.stream().map(CoreExtension::getName).collect(Collectors.joining(", ")));
    }
  }

  private static void ensureNoDuplicateName(Set<CoreExtension> coreExtensions) {
    Map<String, Long> nameCounts = coreExtensions.stream()
      .map(CoreExtension::getName)
      .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
    Set<String> duplicatedNames = nameCounts.entrySet().stream()
      .filter(t -> t.getValue() > 1)
      .map(Map.Entry::getKey)
      .collect(MoreCollectors.toSet());
    checkState(duplicatedNames.isEmpty(),
      "Multiple core extensions declare the following names: %s",
      duplicatedNames.stream().sorted().collect(Collectors.joining(", ")));
  }

  static class ServiceLoaderWrapper {
    Set<CoreExtension> load(ClassLoader classLoader) {
      ServiceLoader<CoreExtension> loader = ServiceLoader.load(CoreExtension.class, classLoader);
      return ImmutableSet.copyOf(loader.iterator());
    }
  }
}
