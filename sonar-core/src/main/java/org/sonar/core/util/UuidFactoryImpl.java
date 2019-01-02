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
package org.sonar.core.util;

import org.apache.commons.codec.binary.Base64;

/**
 */
public enum UuidFactoryImpl implements UuidFactory {

  /**
   * Should be removed as long {@link Uuids} is not used anymore. {@code UuidFactoryImpl}
   * should be built by picocontainer through a public constructor.
   */
  INSTANCE;

  private final UuidGenerator uuidGenerator = new UuidGeneratorImpl();

  @Override
  public String create() {
    return Base64.encodeBase64URLSafeString(uuidGenerator.generate());
  }

}
