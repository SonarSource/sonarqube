/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;

/**
 * NOT thread safe
 * About 10x faster than {@link UuidFactoryImpl}
 * It does not take into account the MAC address to calculate the ids, so it is machine-independent.
 * This class must only be used for testing.
 * @deprecated use {@link UuidFactoryImpl} or {@link SequenceUuidFactory} instead.
 */
@VisibleForTesting
@Deprecated(since = "10.4")
public class UuidFactoryFast implements UuidFactory {
  private static UuidFactoryFast instance = new UuidFactoryFast();

  public static UuidFactoryFast getInstance() {
    return instance;
  }

  private UuidFactoryFast() {
    //
  }

  @Override
  public String create() {
    return UuidFactoryImpl.INSTANCE.create();
  }


}
