/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class ListInterceptor implements LogInterceptor {

  private final List<String> logs = new ArrayList<>();
  private final ListMultimap<LoggerLevel, String> logsByLevel = ArrayListMultimap.create();

  @Override
  public void log(LoggerLevel level, String msg) {
    logs.add(msg);
    logsByLevel.put(level, msg);
  }

  @Override
  public void log(LoggerLevel level, String msg, @Nullable Object arg) {
    String s = ConsoleFormatter.format(msg, arg);
    logs.add(s);
    logsByLevel.put(level, s);
  }

  @Override
  public void log(LoggerLevel level, String msg, @Nullable Object arg1, @Nullable Object arg2) {
    String s = ConsoleFormatter.format(msg, arg1, arg2);
    logs.add(s);
    logsByLevel.put(level, s);
  }

  @Override
  public void log(LoggerLevel level, String msg, Object... args) {
    String s = ConsoleFormatter.format(msg, args);
    logs.add(s);
    logsByLevel.put(level, s);
  }

  @Override
  public void log(LoggerLevel level, String msg, Throwable thrown) {
    logs.add(msg);
    logsByLevel.put(level, msg);
  }

  public List<String> logs() {
    return logs;
  }

  public List<String> logs(LoggerLevel level) {
    return logsByLevel.get(level);
  }

  public void clear() {
    logs.clear();
    logsByLevel.clear();
  }
}
