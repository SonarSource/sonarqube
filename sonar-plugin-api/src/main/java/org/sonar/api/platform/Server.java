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
package org.sonar.api.platform;

import java.io.File;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 2.2
  */
@BatchSide
@ServerSide
@ComputeEngineSide
public abstract class Server {

  public abstract String getId();

  public abstract String getVersion();

  public abstract Date getStartedAt();

  public abstract File getRootDir();

  @CheckForNull
  public abstract File getDeployDir();

  public abstract String getContextPath();

  /**
   * Return the public root url, for instance : https://nemo.sonarqube.org.
   * Default value is {@link org.sonar.api.CoreProperties#SERVER_BASE_URL_DEFAULT_VALUE}
   *
   * @since 5.4
   */
  public abstract String getPublicRootUrl();

  /**
   * The dev mode is enabled when the property sonar.web.dev is true.
   *
   * @since 5.4
   */
  public abstract boolean isDev();

  /**
   * Return whether or not the {#getPublicRootUrl} is started with https.
   *
   * @since 5.4
   */
  public abstract boolean isSecured();

  /**
   * @return the server URL when executed from batch, else null.
   * @since 2.4
   */
  public abstract String getURL();

  /**
   * @since 2.10
   */
  public abstract String getPermanentServerId();
}
