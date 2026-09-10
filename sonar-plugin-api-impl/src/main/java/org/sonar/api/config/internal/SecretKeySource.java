/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.api.config.internal;

import java.util.Optional;

/**
 * A place the secret key of settings encryption can be read from, other than the file pointed at by
 * {@code sonar.secretKeyPath}. Implementations let a deployment supply the key in the way that suits it, for example
 * an environment variable in a container, or a managed key service.
 */
public interface SecretKeySource {

  /**
   * The base64 encoded secret key, or empty when this source holds no key. Returning empty lets the next source be
   * tried, so an implementation must not throw only because it holds nothing.
   */
  Optional<String> loadBase64Key();

  /**
   * Where the key comes from, phrased to be readable inside an error message.
   */
  String describe();
}
