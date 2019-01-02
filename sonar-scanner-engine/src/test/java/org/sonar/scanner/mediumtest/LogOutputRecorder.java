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
package org.sonar.scanner.mediumtest;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.batch.bootstrapper.LogOutput;

public class LogOutputRecorder implements LogOutput {
  private final Multimap<String, String> recordedByLevel = LinkedHashMultimap.create();
  private final List<String> recorded = new LinkedList<>();
  private final StringBuffer asString = new StringBuffer();

  @Override
  public synchronized void log(String formattedMessage, Level level) {
    recordedByLevel.put(level.toString(), formattedMessage);
    recorded.add(formattedMessage);
    asString.append(formattedMessage).append("\n");
  }

  public synchronized Collection<String> getAll() {
    return new ArrayList<>(recorded);
  }

  public synchronized String getAllAsString() {
    return recorded.stream().collect(Collectors.joining("\n"));
  }

  public synchronized Collection<String> get(String level) {
    return new ArrayList<>(recordedByLevel.get(level));
  }

  public synchronized String getAsString() {
    return asString.toString();
  }

}
