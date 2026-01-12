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
package org.sonar.scanner.scan;

import org.sonar.api.Startable;
import org.sonar.scanner.repository.TelemetryCache;

public class ScanPropertiesTelemetryProvider implements Startable {

  private static final String TELEMETRY_JRE_PROVISIONING_DISABLED = "jre_provisioning_disabled";
  private static final String PROPERTY_SCANNER_APP = "sonar.scanner.app";
  private static final String PROPERTY_SCANNER_APP_VERSION = "sonar.scanner.appVersion";
  private static final String PROPERTY_JRE_AUTO_PROVISIONING_DISABLED = "sonar.jreAutoProvisioning.disabled";
  private static final String PROPERTY_SCANNER_SKIP_JRE_PROVISIONING = "sonar.scanner.skipJreProvisioning";
  private static final String PROPERTY_SCANNER_JAVA_EXE_PATH = "sonar.scanner.javaExePath";
  private static final String UNKNOWN_VALUE = "unknown";

  private final ScanProperties scanProperties;
  private final TelemetryCache telemetryCache;

  public ScanPropertiesTelemetryProvider(ScanProperties scanProperties, TelemetryCache telemetryCache) {
    this.scanProperties = scanProperties;
    this.telemetryCache = telemetryCache;
  }

  @Override
  public void start() {
    telemetryCache.put(TELEMETRY_JRE_PROVISIONING_DISABLED, String.valueOf(isProvisioningDisabled()));
    telemetryCache.put(PROPERTY_SCANNER_APP, scanProperties.get(PROPERTY_SCANNER_APP).orElse(UNKNOWN_VALUE));
    telemetryCache.put(PROPERTY_SCANNER_APP_VERSION, scanProperties.get(PROPERTY_SCANNER_APP_VERSION).orElse(UNKNOWN_VALUE));
  }

  private boolean isProvisioningDisabled() {
    return scanProperties.get(PROPERTY_JRE_AUTO_PROVISIONING_DISABLED).map(Boolean::valueOf).orElse(false)
      || scanProperties.get(PROPERTY_SCANNER_SKIP_JRE_PROVISIONING).map(Boolean::valueOf).orElse(false)
      || scanProperties.get(PROPERTY_SCANNER_JAVA_EXE_PATH).isPresent();
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
