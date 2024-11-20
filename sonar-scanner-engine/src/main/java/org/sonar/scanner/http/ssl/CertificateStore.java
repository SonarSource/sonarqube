/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.http.ssl;

import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;

public class CertificateStore {
  public static final String DEFAULT_PASSWORD = "changeit";
  /**
   * @deprecated it was a bad decision to use this value as default password, as the keytool utility requires a password to be at least 6 characters long
   */
  @Deprecated(since = "11.4")
  public static final String OLD_DEFAULT_PASSWORD = "sonar";
  public static final String DEFAULT_STORE_TYPE = "PKCS12";
  private final Path path;
  private final String keyStorePassword;
  private final String keyStoreType;

  public CertificateStore(Path path, @Nullable String keyStorePassword) {
    this.path = path;
    this.keyStorePassword = keyStorePassword;
    this.keyStoreType = DEFAULT_STORE_TYPE;
  }

  public Path getPath() {
    return path;
  }

  public Optional<String> getKeyStorePassword() {
    return Optional.ofNullable(keyStorePassword);
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }
}
