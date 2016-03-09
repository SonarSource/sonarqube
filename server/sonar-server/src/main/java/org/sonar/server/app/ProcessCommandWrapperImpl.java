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
package org.sonar.server.app;

import java.io.File;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessCommands;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class ProcessCommandWrapperImpl implements ProcessCommandWrapper {
  private static final Logger LOG = Loggers.get(ProcessCommandWrapperImpl.class);

  private final Settings settings;

  public ProcessCommandWrapperImpl(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void requestSQRestart() {
    call(VoidMethod.ASK_FOR_RESTART);
  }

  @Override
  public void notifyOperational() {
    call(VoidMethod.SET_OPERATIONAL);
  }

  private void call(VoidMethod command) {
    File shareDir = nonNullValueAsFile(PROPERTY_SHARED_PATH);
    int processNumber = nonNullAsInt(PROPERTY_PROCESS_INDEX);
    try (ProcessCommands commands = new DefaultProcessCommands(shareDir, processNumber, false)) {
      command.callOn(commands);
    } catch (Exception e) {
      LOG.warn("Failed to close ProcessCommands", e);
    }
  }

  private enum VoidMethod {
    SET_OPERATIONAL() {
      @Override
      void callOn(ProcessCommands processCommands) {
        processCommands.setOperational();
      }
    },
    ASK_FOR_RESTART() {
      @Override
      void callOn(ProcessCommands processCommands) {
        processCommands.askForRestart();
      }
    };

    abstract void callOn(ProcessCommands processCommands);
  }

  private int nonNullAsInt(String key) {
    String s = settings.getString(key);
    checkArgument(s != null, "Property %s is not set", key);
    return Integer.parseInt(s);
  }

  public File nonNullValueAsFile(String key) {
    String s = settings.getString(key);
    checkArgument(s != null, "Property %s is not set", key);
    return new File(s);
  }
}
