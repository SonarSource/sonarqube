/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import org.sonar.api.Startable;

/*
 * The only purpose of this class is to start MyBatis.
 * MyBatis is not Startable because in the unit tests it's cached and added to the container, and in that situation we don't want it to be started.
 */
public class StartMyBatis implements Startable {
  private final MyBatis myBatis;

  public StartMyBatis(MyBatis myBatis) {
    this.myBatis = myBatis;
  }

  public void start() {
    myBatis.start();
  }

  public void stop() {
    // nothing to do
  }
}
