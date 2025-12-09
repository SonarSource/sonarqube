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
package org.sonar.server.property;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Allows to read and write internal properties.
 */
public interface InternalProperties {

  String SERVER_ID_CHECKSUM = "server.idChecksum";

  /**
   * Compute Engine is pausing/paused if property value is "true".
   */
  String COMPUTE_ENGINE_PAUSE = "ce.pause";

  String BITBUCKETCLOUD_APP_SHAREDSECRET = "bbc.app.sharedSecret";

  /**
   * First installation date
   */
  String INSTALLATION_DATE = "installation.date";

  /**
   * first installation SQ version
   */
  String INSTALLATION_VERSION = "installation.version";

  /**
   * Default permission templates
   */
  String DEFAULT_PROJECT_TEMPLATE = "defaultTemplate.prj";
  String DEFAULT_PORTFOLIO_TEMPLATE = "defaultTemplate.port";
  String DEFAULT_APPLICATION_TEMPLATE = "defaultTemplate.app";

  String DEFAULT_ADMIN_CREDENTIAL_USAGE_EMAIL = "default.admin.cred";

  /**
   * Read the value of the specified property.
   *
   * @return {@link Optional#empty()} if the property does not exist, an empty string if the property is empty,
   *         otherwise the value of the property as a String.
   *
   * @throws IllegalArgumentException if {@code propertyKey} is {@code null} or empty
   */
  Optional<String> read(String propertyKey);

  /**
   * Write the value of the specified property.
   * <p>
   *   {@code null} and empty string are valid values which will persist the specified property as empty.
   * </p>
   *
   * @throws IllegalArgumentException if {@code propertyKey} is {@code null} or empty
   */
  void write(String propertyKey, @Nullable String value);

  /**
   * Delete the specified property.
   *
   */
  void delete(String propertyKey);
}
