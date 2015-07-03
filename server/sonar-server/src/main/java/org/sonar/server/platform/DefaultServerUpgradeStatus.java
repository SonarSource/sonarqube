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
package org.sonar.server.platform;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.picocontainer.Startable;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.version.DatabaseVersion;

/**
 * @since 2.5
 */
public final class DefaultServerUpgradeStatus implements ServerUpgradeStatus, Startable {

  private final DatabaseVersion dbVersion;

  // available when connected to db
  private int initialDbVersion;

  public DefaultServerUpgradeStatus(DatabaseVersion dbVersion) {
    this.dbVersion = dbVersion;
  }

  @Override
  public void start() {
    Integer v = dbVersion.getVersion();
    this.initialDbVersion = (v != null ? v : -1);
  }

  @Override
  public void stop() {
    // do nothing
  }

  @Override
  public boolean isUpgraded() {
    return !isFreshInstall() && (initialDbVersion < DatabaseVersion.LAST_VERSION);
  }

  @Override
  public boolean isFreshInstall() {
    return initialDbVersion < 0;
  }

  @Override
  public int getInitialDbVersion() {
    return initialDbVersion;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
