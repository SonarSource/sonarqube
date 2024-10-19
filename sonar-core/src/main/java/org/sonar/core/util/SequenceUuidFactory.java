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
package org.sonar.core.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Only for tests. This implementation of {@link UuidFactory} generates
 * ids with the structure of a UUID v4, but containing a sequence of integers ("1", "2", ...). It starts with "1".
 */
@VisibleForTesting
public class SequenceUuidFactory implements UuidFactory {
  private static final String UUID_PREFIX = "00000000-0000-0000-0000-";

  public static final String UUID_1 = UUID_PREFIX + "000000000001";
  public static final String UUID_2 = UUID_PREFIX + "000000000002";
  public static final String UUID_3 = UUID_PREFIX + "000000000003";

  private final AtomicInteger id = new AtomicInteger(1);

  @Override
  public String create() {
    int currentId = id.getAndIncrement();
    return UUID_PREFIX + StringUtils.leftPad(String.valueOf(currentId), 12, "0");
  }
}
