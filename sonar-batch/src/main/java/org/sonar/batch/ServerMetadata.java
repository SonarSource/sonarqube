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
package org.sonar.batch;

import org.sonar.api.BatchComponent;

import java.util.Date;

/**
 * @deprecated in 3.4. Replaced by {@link org.sonar.batch.bootstrap.ServerMetadata}. Plugins should use {@link org.sonar.api.platform.Server}.
 */
@Deprecated
public class ServerMetadata implements BatchComponent {
  private org.sonar.batch.bootstrap.ServerMetadata metadata;

  public ServerMetadata(org.sonar.batch.bootstrap.ServerMetadata metadata) {
    this.metadata = metadata;
  }

  public String getId() {
    return metadata.getId();
  }

  public String getVersion() {
    return metadata.getVersion();
  }

  public Date getStartedAt() {
    return metadata.getStartedAt();
  }

  public String getURL() {
    return metadata.getURL();
  }

  public String getPermanentServerId() {
    return metadata.getPermanentServerId();
  }
}
