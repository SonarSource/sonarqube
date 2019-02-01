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
package org.sonar.scanner.scan;

import org.picocontainer.annotations.Nullable;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.RawScannerProperties;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.sonar.core.config.ScannerProperties.DISABLE_PROJECT_AND_ORG_AUTODETECTION;

public class ExternalProjectKeyAndOrganizationProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ExternalProjectKeyAndOrganizationProvider.class);
  private ExternalProjectKeyAndOrganization properties = null;

  public ExternalProjectKeyAndOrganization provide(RawScannerProperties rawScannerProperties,
    @Nullable @javax.annotation.Nullable ExternalProjectKeyAndOrganizationLoader loader) {
    if (properties == null) {
      boolean disableProjectKeyAndOrgAutodetection = equalsIgnoreCase(
        rawScannerProperties.property(DISABLE_PROJECT_AND_ORG_AUTODETECTION), "true");
      if (disableProjectKeyAndOrgAutodetection) {
        LOG.info("Skipping project and organization key auto-detection.");
      }

      if (loader != null && !disableProjectKeyAndOrgAutodetection) {
        properties = loader.load().orElse(new EmptyExternalProjectKeyAndOrganization());
      } else {
        properties = new EmptyExternalProjectKeyAndOrganization();
      }
    }

    return properties;
  }
}
