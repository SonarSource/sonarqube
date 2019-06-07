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
package org.sonar.scanner.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.sonar.scanner.scan.ExternalProjectKeyAndOrganization;

import static org.sonar.api.CoreProperties.PROJECT_KEY_PROPERTY;

/**
 * Properties that are coming from scanner.
 */
@Immutable
public class ProcessedScannerProperties {

  private final Map<String, String> properties;

  public ProcessedScannerProperties(RawScannerProperties rawScannerProperties,
    ExternalProjectKeyAndOrganization externalProjectKeyAndOrganization) {
    this.properties = new HashMap<>();
    this.properties.putAll(rawScannerProperties.properties());

    externalProjectKeyAndOrganization.getProjectKey()
      .ifPresent(projectKey -> properties.put(PROJECT_KEY_PROPERTY, projectKey));
    externalProjectKeyAndOrganization.getOrganization()
      .ifPresent(organization -> properties.put(org.sonar.core.config.ScannerProperties.ORGANIZATION, organization));
  }

  public Map<String, String> properties() {
    return Collections.unmodifiableMap(new HashMap<>(properties));
  }

  public String property(String key) {
    return properties.get(key);
  }

  public String getProjectKey() {
    return this.properties.get(PROJECT_KEY_PROPERTY);
  }
}
