/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.platform;

import java.util.Date;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * Runtime information about server
 *
 * @since 2.2
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public abstract class Server {

  /**
   * UUID identifying the installation. It is persisted
   * so that it does not change over time, even after
   * a restart.
   * In the context of cluster, the value is shared
   * by all the nodes.
   *
   * @return a non-null UUID. Format can change over versions.
   */
  public abstract String getId();

  /**
   * Since 6.7, it returns exactly {@link #getId()}. In previous
   * versions it returned ab UUID generated on demand by system
   * administrators and may be null.
   *
   * @deprecated replaced by {@link #getId()} in 6.7.
   * @since 2.10
   */
  @Deprecated
  public abstract String getPermanentServerId();

  /**
   * Non-null version of SonarQube at runtime
   */
  public abstract String getVersion();

  /**
   * Date when server started. In the context of cluster, this is the
   * date of the startup of the first node. Value is the same on all
   * cluster nodes.
   */
  public abstract Date getStartedAt();

  /**
   * Context path of web server. Value is blank {@code ""} by default. When defined by
   * the property {@code sonar.web.context} of conf/sonar.properties, then value starts but does
   * not end with slash {@code '/'}, for instance {@code "/sonarqube"}.
   *
   * @return non-null but possibly blank path
   */
  public abstract String getContextPath();

  /**
   * Return the public root url, without trailing slash, for instance : https://nemo.sonarqube.org.
   *
   * @since 5.4
   */
  public abstract String getPublicRootUrl();

  /**
   * Return whether or not the {#getPublicRootUrl} is started with https.
   *
   * @since 5.4
   * @deprecated since 5.6, use instead {@link javax.servlet.http.HttpServletRequest#getHeader(String)} and check that X-Forwarded-Proto header is set to "https".
   */
  @Deprecated
  public abstract boolean isSecured();
}
