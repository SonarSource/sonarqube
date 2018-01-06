/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.web.requestid;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.sonar.core.util.UuidGenerator;

/**
 * This implementation of {@link RequestIdGenerator} creates unique identifiers for HTTP requests leveraging
 * {@link UuidGenerator.WithFixedBase#generate(int)} and a counter of HTTP requests.
 * <p>
 * To work around the limit of unique values produced by {@link UuidGenerator.WithFixedBase#generate(int)}, the
 * {@link UuidGenerator.WithFixedBase} instance will be renewed every
 * {@link RequestIdConfiguration#getUidGeneratorRenewalCount() RequestIdConfiguration#uidGeneratorRenewalCount}
 * HTTP requests.
 * </p>
 * <p>
 * This implementation is Thread safe.
 * </p>
 */
public class RequestIdGeneratorImpl implements RequestIdGenerator {
  /**
   * The value to which the HTTP request count will be compared to (using a modulo operator,
   * see {@link #mustRenewUuidGenerator(long)}).
   *
   * <p>
   * This value can't be the last value before {@link UuidGenerator.WithFixedBase#generate(int)} returns a non unique
   * value, ie. 2^23-1 because there is no guarantee the renewal will happen before any other thread calls
   * {@link UuidGenerator.WithFixedBase#generate(int)} method of the deplated {@link UuidGenerator.WithFixedBase} instance.
   * </p>
   *
   * <p>
   * To keep a comfortable margin of error, 2^22 will be used.
   * </p>
   */
  public static final long UUID_GENERATOR_RENEWAL_COUNT = 4_194_304;

  private final AtomicLong counter = new AtomicLong();
  private final RequestIdGeneratorBase requestIdGeneratorBase;
  private final RequestIdConfiguration requestIdConfiguration;
  private final AtomicReference<UuidGenerator.WithFixedBase> uuidGenerator;

  public RequestIdGeneratorImpl(RequestIdGeneratorBase requestIdGeneratorBase, RequestIdConfiguration requestIdConfiguration) {
    this.requestIdGeneratorBase = requestIdGeneratorBase;
    this.uuidGenerator  = new AtomicReference<>(requestIdGeneratorBase.createNew());
    this.requestIdConfiguration = requestIdConfiguration;
  }

  @Override
  public String generate() {
    UuidGenerator.WithFixedBase currentUuidGenerator = this.uuidGenerator.get();
    long counterValue = counter.getAndIncrement();
    if (counterValue != 0 && mustRenewUuidGenerator(counterValue)) {
      UuidGenerator.WithFixedBase newUuidGenerator = requestIdGeneratorBase.createNew();
      uuidGenerator.set(newUuidGenerator);
      return generate(newUuidGenerator, counterValue);
    }
    return generate(currentUuidGenerator, counterValue);
  }

  /**
   * Since renewal of {@link UuidGenerator.WithFixedBase} instance is based on the HTTP request counter, only a single
   * thread can get the right value which will make this method return true. So, this is thread-safe by design, therefor
   * this method doesn't need external synchronization.
   * <p>
   * The value to which the counter is compared should however be chosen with caution: see {@link #UUID_GENERATOR_RENEWAL_COUNT}.
   * </p>
   */
  private boolean mustRenewUuidGenerator(long counter) {
    return counter % requestIdConfiguration.getUidGeneratorRenewalCount() == 0;
  }

  private static String generate(UuidGenerator.WithFixedBase uuidGenerator, long increment) {
    return Base64.getEncoder().encodeToString(uuidGenerator.generate((int) increment));
  }

}
