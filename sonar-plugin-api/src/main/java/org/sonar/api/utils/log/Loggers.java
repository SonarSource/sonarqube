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
package org.sonar.api.utils.log;

public abstract class Loggers {

  static Loggers getFactory() {
    return LoggerFactory.getFactory();
  }

  protected abstract Logger newInstance(String name);

  protected abstract LoggerLevel getLevel();

  protected abstract void setLevel(LoggerLevel level);

  public static Logger get(Class<?> name) {
    return LoggerFactory.get(name);
  }

  public static Logger get(String name) {
    return LoggerFactory.get(name);
  }

}
