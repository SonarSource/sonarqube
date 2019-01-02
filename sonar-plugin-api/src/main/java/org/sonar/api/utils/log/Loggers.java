/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

/**
 * @since 5.1
 */
public abstract class Loggers {

  private static volatile Loggers factory;

  static {
    try {
      Class.forName("ch.qos.logback.classic.Logger");
      factory = new LogbackLoggers();
    } catch (Throwable e) {
      // no slf4j -> testing environment
      factory = new ConsoleLoggers();
    }
  }

  public static Logger get(Class<?> name) {
    return factory.newInstance(name.getName());
  }

  public static Logger get(String name) {
    return factory.newInstance(name);
  }

  static Loggers getFactory() {
    return factory;
  }

  protected abstract Logger newInstance(String name);

  protected abstract LoggerLevel getLevel();

  protected abstract void setLevel(LoggerLevel level);

}
