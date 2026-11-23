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
package org.sonar.process.es;

/**
 * Conservative HTTP connection reuse policy shared by every SonarQube client of Elasticsearch (Web and Compute
 * Engine through {@code org.sonar.server.es.EsClient}, the main process through {@code EsConnectorImpl}).
 * <p>
 * Pooled HTTP connections to Elasticsearch can stay idle for a long time (e.g. a quiet night with no analysis).
 * Anything between the client and the node - a NAT gateway, a load balancer, a firewall, conntrack in a container
 * network - may then silently forget the connection. Reusing it fails the next request, and since searches and bulks
 * are {@code POST}s they are not retried by the HTTP client. The policy below avoids this by:
 * <ul>
 *   <li>never reusing a connection that has been idle longer than {@link #MAX_IDLE_REUSE_SECONDS}, which is below
 *   the idle timeouts of common middleboxes (Azure load balancer/NAT: 4 min, AWS NLB: 350 s). Elasticsearch does
 *   not send a {@code Keep-Alive} response header, so without it HttpClient 5 would keep connections for 3 min;</li>
 *   <li>checking that a connection idle for more than {@link #VALIDATE_AFTER_INACTIVITY_SECONDS} has not been closed
 *   by the peer before reusing it;</li>
 *   <li>enabling TCP keep-alive with per-socket timings, so that the connection is kept alive in middleboxes and a dead
 *   peer is detected quickly, without depending on the OS defaults ({@code net.ipv4.tcp_keepalive_time} is 2 hours
 *   on Linux and often cannot be changed from inside a container).</li>
 * </ul>
 */
public final class EsConnectionReusePolicy {

  /** A pooled connection idle for longer than this is closed instead of being reused. */
  public static final int MAX_IDLE_REUSE_SECONDS = 60;
  /** A pooled connection idle for longer than this is checked for staleness before being reused. */
  public static final int VALIDATE_AFTER_INACTIVITY_SECONDS = 2;
  /** Idle time before the first TCP keep-alive probe ({@code TCP_KEEPIDLE}). */
  public static final int TCP_KEEP_IDLE_SECONDS = 30;
  /** Interval between TCP keep-alive probes ({@code TCP_KEEPINTERVAL}). */
  public static final int TCP_KEEP_INTERVAL_SECONDS = 10;
  /** Number of unanswered TCP keep-alive probes before the connection is considered dead ({@code TCP_KEEPCOUNT}). */
  public static final int TCP_KEEP_COUNT = 3;

  private EsConnectionReusePolicy() {
    // constants only
  }
}
