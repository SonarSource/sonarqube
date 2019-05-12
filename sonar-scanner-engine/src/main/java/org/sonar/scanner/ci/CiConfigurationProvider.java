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
package org.sonar.scanner.ci;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class CiConfigurationProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(CiConfigurationProvider.class);
  private static final String PROP_DISABLED = "sonar.ci.autoconfig.disabled";

  public CiConfiguration provide(Configuration configuration, CiVendor[] ciVendors) {
    boolean disabled = configuration.getBoolean(PROP_DISABLED).orElse(false);
    if (disabled) {
      return new EmptyCiConfiguration();
    }

    List<CiVendor> detectedVendors = Arrays.stream(ciVendors)
      .filter(CiVendor::isDetected)
      .collect(Collectors.toList());

    if (detectedVendors.size() > 1) {
      List<String> names = detectedVendors.stream().map(CiVendor::getName).collect(Collectors.toList());
      throw MessageException.of("Multiple CI environments are detected: " + names + ". Please check environment variables or set property " + PROP_DISABLED + " to true.");
    }

    if (detectedVendors.size() == 1) {
      CiVendor vendor = detectedVendors.get(0);
      LOG.info("Detected {}", vendor.getName());
      return vendor.loadConfiguration();
    }
    return new EmptyCiConfiguration();
  }

  private static class EmptyCiConfiguration implements CiConfiguration {
    @Override
    public Optional<String> getScmRevision() {
      return Optional.empty();
    }
  }
}
