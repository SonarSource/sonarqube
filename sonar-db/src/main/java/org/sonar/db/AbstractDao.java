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
package org.sonar.db;

import org.sonar.api.utils.System2;

public abstract class AbstractDao implements Dao {

  private final MyBatis myBatis;
  private final System2 system2;

  public AbstractDao(MyBatis myBatis, System2 system2) {
    this.myBatis = myBatis;
    this.system2 = system2;
  }

  protected MyBatis myBatis() {
    return myBatis;
  }

  protected long now() {
    return system2.now();
  }
}
