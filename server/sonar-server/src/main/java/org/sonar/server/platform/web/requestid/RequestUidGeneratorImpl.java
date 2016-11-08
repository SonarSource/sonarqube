/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.web.requestid;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import org.sonar.core.util.UuidGeneratorImpl;

/**
 * This implementation of {@link RequestUidGenerator} creates unique identifiers for HTTP requests leveraging
 * {@link UuidGeneratorImpl#withFixedBase()}.
 * <p>
 * This implementation is Thread safe.
 * </p>
 */
public class RequestUidGeneratorImpl implements RequestUidGenerator {
  private final AtomicInteger counter = new AtomicInteger();
  private final RequestUidGeneratorBase requestUidGeneratorBase;

  public RequestUidGeneratorImpl(RequestUidGeneratorBase requestUidGeneratorBase) {
    this.requestUidGeneratorBase = requestUidGeneratorBase;
  }

  @Override
  public String generate() {
    return Base64.getEncoder().encodeToString(requestUidGeneratorBase.generate(counter.incrementAndGet()));
  }

}
