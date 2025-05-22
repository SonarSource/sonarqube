/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.xoo.global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

/**
 * Sensor that throws a {@link java.lang.Error} during execution.
 */
public class ErrorThrowingSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(ErrorThrowingSensor.class);

  public static final String ENABLE_PROP = "sonar.scanner.errorSensor";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("Error Throwing Sensor")
      .onlyWhenConfiguration(c -> c.hasKey(ENABLE_PROP));
  }

  @Override
  public void execute(SensorContext context) {
    LOG.info("Running Error Throwing sensor");
    runNonDaemonThread();
    throw new XooError("This is thrown by the ErrorThrowing Sensor, it's its job to throw it!");
  }

  private static void runNonDaemonThread() {
    Thread nonDaemonThread = new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
    LOG.info("Starting non-daemon Thread");
    nonDaemonThread.start();
  }

  static class XooError extends Error {
    public XooError(String message) {
      super(message);
    }
  }
}
