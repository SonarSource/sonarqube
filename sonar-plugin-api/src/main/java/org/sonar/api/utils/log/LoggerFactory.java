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

/**
 * @since 8.7
 */
public abstract class LoggerFactory {

  private static Loggers factory;

  private LoggerFactory() {
    // no new instance
  }

  static {
    try {
      Class.forName("ch.qos.logback.classic.Logger");
      factory = new LogbackLoggers();
    } catch (Exception e) {
      // no slf4j -> testing environment
      factory = new ConsoleLoggers();
    }
  }

  static Loggers getFactory(){
    return factory;
  }

  static Logger get(Class<?> name) {
    return factory.newInstance(name.getName());
  }

  static Logger get(String name) {
    return factory.newInstance(name);
  }

}
