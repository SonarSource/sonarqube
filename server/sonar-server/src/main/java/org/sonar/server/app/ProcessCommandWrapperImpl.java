/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.app;

import java.io.File;
import org.sonar.api.config.Configuration;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessCommands;

import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class ProcessCommandWrapperImpl implements ProcessCommandWrapper {
  private final Configuration config;

  public ProcessCommandWrapperImpl(Configuration config) {
    this.config = config;
  }

  @Override
  public void requestSQRestart() {
    call(VoidMethod.ASK_FOR_RESTART, selfProcessNumber());
  }

  @Override
  public void requestStop() {
    call(VoidMethod.ASK_FOR_STOP, selfProcessNumber());
  }

  @Override
  public void notifyOperational() {
    call(VoidMethod.SET_OPERATIONAL, selfProcessNumber());
  }

  private int selfProcessNumber() {
    return nonNullAsInt(PROPERTY_PROCESS_INDEX);
  }

  private <T> T call(VoidMethod command, int processNumber) {
    File shareDir = nonNullValueAsFile(PROPERTY_SHARED_PATH);
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(shareDir, processNumber)) {
      return command.callOn(commands);
    }
  }

  private enum VoidMethod {
    SET_OPERATIONAL() {
      @Override
      <T> T callOn(ProcessCommands processCommands) {
        processCommands.setOperational();
        return null;
      }
    },
    ASK_FOR_RESTART() {
      @Override
      <T> T callOn(ProcessCommands processCommands) {
        processCommands.askForRestart();
        return null;
      }
    },
    ASK_FOR_STOP() {
      @Override
      <T> T callOn(ProcessCommands processCommands) {
        processCommands.askForStop();
        return null;
      }
    };
    abstract <T> T callOn(ProcessCommands processCommands);
  }

  private int nonNullAsInt(String key) {
    return config.getInt(key).orElseThrow(() -> new IllegalArgumentException(String.format("Property %s is not set", key)));
  }

  private File nonNullValueAsFile(String key) {
    return new File(config.get(key).orElseThrow(() -> new IllegalArgumentException(String.format("Property %s is not set", key))));
  }
}
