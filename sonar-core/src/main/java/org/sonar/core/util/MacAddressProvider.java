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
package org.sonar.core.util;

import com.google.common.annotations.VisibleForTesting;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Enumeration;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Used by {@link UuidFactoryImpl}. Heavily inspired by https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/common/MacAddressProvider.java
 */
class MacAddressProvider {

  private static final Logger LOGGER = Loggers.get(MacAddressProvider.class);
  public static final int BYTE_SIZE = 6;

  private MacAddressProvider() {
    // only static stuff
  }

  public static byte[] getSecureMungedAddress() {
    byte[] address = null;
    try {
      address = getMacAddress();
    } catch (SocketException se) {
      LOGGER.warn("Unable to get mac address, will use a dummy address", se);
      // address will be set below
    }

    if (!isValidAddress(address)) {
      LOGGER.warn("Unable to get a valid mac address, will use a dummy address");
      address = constructDummyMulticastAddress();
    }

    byte[] mungedBytes = new byte[BYTE_SIZE];
    new SecureRandom().nextBytes(mungedBytes);
    for (int i = 0; i < BYTE_SIZE; ++i) {
      mungedBytes[i] ^= address[i];
    }

    return mungedBytes;
  }

  private static boolean isValidAddress(@Nullable byte[] address) {
    if (address == null || address.length != BYTE_SIZE) {
      return false;
    }
    for (byte b : address) {
      if (b != 0x00) {
        // If any of the bytes are non zero assume a good address
        return true;
      }
    }
    return false;
  }

  @CheckForNull
  private static byte[] getMacAddress() throws SocketException {
    Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
    if (en != null) {
      while (en.hasMoreElements()) {
        NetworkInterface nint = en.nextElement();
        if (!nint.isLoopback()) {
          // Pick the first valid non loopback address we find
          byte[] address = nint.getHardwareAddress();
          if (isValidAddress(address)) {
            return address;
          }
        }
      }
    }
    // Could not find a mac address
    return null;
  }

  @VisibleForTesting
  static byte[] constructDummyMulticastAddress() {
    byte[] dummy = new byte[BYTE_SIZE];
    new SecureRandom().nextBytes(dummy);
    // Set the broadcast bit to indicate this is not a _real_ mac address
    dummy[0] |= (byte) 0x01;
    return dummy;
  }

}
