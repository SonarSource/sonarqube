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
package org.sonarqube.ws.client;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * {@link SSLSocketFactory} which enables all the versions of TLS. This is required
 * to support TLSv1.2 on Java 7. Note that Java 8 supports TLSv1.2 natively, without
 * any configuration
 */
public class Tls12Java7SocketFactory extends SSLSocketFactory {

  @VisibleForTesting
  static final String[] TLS_PROTOCOLS = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};

  private final SSLSocketFactory delegate;

  public Tls12Java7SocketFactory(SSLSocketFactory delegate) {
    this.delegate = delegate;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return delegate.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return delegate.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
    Socket underlyingSocket = delegate.createSocket(socket, host, port, autoClose);
    return overrideProtocol(underlyingSocket);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    Socket underlyingSocket = delegate.createSocket(host, port);
    return overrideProtocol(underlyingSocket);
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
    Socket underlyingSocket = delegate.createSocket(host, port, localAddress, localPort);
    return overrideProtocol(underlyingSocket);
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    Socket underlyingSocket = delegate.createSocket(host, port);
    return overrideProtocol(underlyingSocket);
  }

  @Override
  public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
    Socket underlyingSocket = delegate.createSocket(host, port, localAddress, localPort);
    return overrideProtocol(underlyingSocket);
  }

  /**
   * Enables TLS v1.0, 1.1 and 1.2 on the socket 
   */
  private static Socket overrideProtocol(Socket socket) {
    if (socket instanceof SSLSocket) {
      ((SSLSocket) socket).setEnabledProtocols(TLS_PROTOCOLS);
    }
    return socket;
  }
}
