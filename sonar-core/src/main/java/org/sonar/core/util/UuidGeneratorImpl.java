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

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class UuidGeneratorImpl implements UuidGenerator {

  private final FullNewUuidGenerator fullNewUuidGenerator = new FullNewUuidGenerator();

  @Override
  public byte[] generate() {
    return fullNewUuidGenerator.get();
  }

  @Override
  public WithFixedBase withFixedBase() {
    return new FixedBasedUuidGenerator();
  }

  private static class UuidGeneratorBase {
    // We only use bottom 3 bytes for the sequence number. Paranoia: init with random int so that if JVM/OS/machine goes down, clock slips
    // backwards, and JVM comes back up, we are less likely to be on the same sequenceNumber at the same time:
    private final AtomicInteger sequenceNumber = new AtomicInteger(new SecureRandom().nextInt());
    private final byte[] secureMungedAddress = MacAddressProvider.getSecureMungedAddress();
    // Used to ensure clock moves forward
    private long lastTimestamp = 0L;

    void initBase(byte[] buffer, int sequenceId) {
      long timestamp = System.currentTimeMillis();

      synchronized (this) {
        // Don't let timestamp go backwards, at least "on our watch" (while this JVM is running). We are still vulnerable if we are
        // shut down, clock goes backwards, and we restart... for this we randomize the sequenceNumber on init to decrease chance of
        // collision:
        timestamp = Math.max(lastTimestamp, timestamp);

        if (sequenceId == 0) {
          // Always force the clock to increment whenever sequence number is 0, in case we have a long time-slip backwards:
          timestamp++;
        }

        lastTimestamp = timestamp;
      }

      // Only use lower 6 bytes of the timestamp (this will suffice beyond the year 10000):
      putLong(buffer, timestamp, 0, 6);

      // MAC address adds 6 bytes:
      System.arraycopy(secureMungedAddress, 0, buffer, 6, secureMungedAddress.length);
    }

    protected byte[] generate(byte[] buffer, int increment) {
      // Sequence number adds 3 bytes
      putLong(buffer, increment, 12, 3);

      return buffer;
    }

    int getSequenceId() {
      return sequenceNumber.incrementAndGet() & 0xffffff;
    }

    /** Puts the lower numberOfLongBytes from l into the array, starting index pos. */
    private static void putLong(byte[] array, long l, int pos, int numberOfLongBytes) {
      for (int i = 0; i < numberOfLongBytes; ++i) {
        array[pos + numberOfLongBytes - i - 1] = (byte) (l >>> (i * 8));
      }
    }
  }

  private static final class FullNewUuidGenerator extends UuidGeneratorBase implements Supplier<byte[]> {

    @Override
    public byte[] get() {
      byte[] buffer = new byte[15];
      int sequenceId = getSequenceId();
      initBase(buffer, sequenceId);
      return super.generate(buffer, sequenceId);
    }
  }

  private static class FixedBasedUuidGenerator extends UuidGeneratorBase implements WithFixedBase {
    private final byte[] base = new byte[15];

    FixedBasedUuidGenerator() {
      int sequenceId = getSequenceId();
      initBase(base, sequenceId);
    }

    @Override
    public byte[] generate(int increment) {
      byte[] buffer = new byte[15];
      System.arraycopy(base, 0, buffer, 0, buffer.length);
      return super.generate(buffer, increment);
    }
  }
}
