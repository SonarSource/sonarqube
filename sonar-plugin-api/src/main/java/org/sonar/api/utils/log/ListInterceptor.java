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
package org.sonar.api.utils.log;

import java.util.ArrayList;
import java.util.List;

class ListInterceptor extends LogInterceptor {

  private final List<String> logs = new ArrayList<>();

  @Override
  public void log(String msg) {
    logs.add(msg);
  }

  @Override
  public void log(String msg, Object arg) {
    logs.add(ConsoleFormatter.format(msg, arg));
  }

  @Override
  public void log(String msg, Object arg1, Object arg2) {
    logs.add(ConsoleFormatter.format(msg, arg1, arg2));
  }

  @Override
  public void log(String msg, Object... args) {
    logs.add(ConsoleFormatter.format(msg, args));
  }

  @Override
  public void log(String msg, Throwable thrown) {
    logs.add(msg);
  }

  public List<String> logs() {
    return logs;
  }
}
