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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class ListInterceptor implements LogInterceptor {

  private final List<LogAndArguments> logs = new ArrayList<>();
  private final ListMultimap<LoggerLevel, LogAndArguments> logsByLevel = ArrayListMultimap.create();

  @Override
  public void log(LoggerLevel level, String msg) {
    LogAndArguments l = new LogAndArguments(msg, msg);
    add(level, l);
  }

  @Override
  public void log(LoggerLevel level, String msg, @Nullable Object arg) {
    String s = ConsoleFormatter.format(msg, arg);
    LogAndArguments l = new LogAndArguments(s, msg, arg);
    add(level, l);
  }

  @Override
  public void log(LoggerLevel level, String msg, @Nullable Object arg1, @Nullable Object arg2) {
    String s = ConsoleFormatter.format(msg, arg1, arg2);
    LogAndArguments l = new LogAndArguments(s, msg, arg1, arg2);
    add(level, l);
  }

  @Override
  public void log(LoggerLevel level, String msg, Object... args) {
    String s = ConsoleFormatter.format(msg, args);
    LogAndArguments l = new LogAndArguments(s, msg, args);
    add(level, l);
  }

  @Override
  public void log(LoggerLevel level, String msg, Throwable thrown) {
    LogAndArguments l = new LogAndArguments(msg, msg, thrown);
    add(level, l);
  }

  private void add(LoggerLevel level, LogAndArguments l) {
    logs.add(l);
    logsByLevel.put(level, l);
  }

  public List<String> logs() {
    return logs.stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
  }

  public Optional<String> findFirst(Predicate<String> logPredicate) {
    return logs.stream().map(LogAndArguments::getFormattedMsg).filter(logPredicate).findFirst();
  }

  public List<String> logs(LoggerLevel level) {
    return logsByLevel.get(level).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.toList());
  }

  public List<LogAndArguments> getLogs() {
    return logs;
  }

  public List<LogAndArguments> getLogs(LoggerLevel level) {
    return logsByLevel.get(level);
  }

  public void clear() {
    logs.clear();
    logsByLevel.clear();
  }

}
