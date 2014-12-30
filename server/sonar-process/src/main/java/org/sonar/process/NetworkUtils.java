/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class NetworkUtils {

  private NetworkUtils() {
    // only static stuff
  }

  /**
   * Get an unused port
   */
  public static int freePort() {
    ServerSocket socket = null;
    try  {
      socket = new ServerSocket();
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(0));
      return socket.getLocalPort();

    } catch (IOException e) {
      throw new IllegalStateException("Can not find a free network port", e);

    } finally {
      IOUtils.closeQuietly(socket);
    }
  }
}
