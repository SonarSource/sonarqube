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
package org.sonar.api.platform;

import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.Date;

/**
 * @since 2.2
  */
@BatchSide
@ServerSide
public abstract class Server {

  public abstract String getId();

  public abstract String getVersion();

  public abstract Date getStartedAt();

  public abstract File getRootDir();

  @CheckForNull
  public abstract File getDeployDir();

  public abstract String getContextPath();

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
