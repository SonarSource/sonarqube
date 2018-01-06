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
package ce;

import java.io.File;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class CePauseStep implements MeasureComputer {

  private static final Logger LOGGER = Loggers.get(CePauseStep.class);

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
      .setInputMetrics("ncloc")
      .setOutputMetrics(PauseMetric.KEY)
      .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getComponent().getType() == Component.Type.PROJECT) {
      String path = context.getSettings().getString("sonar.ce.pauseTask.path");
      if (path != null) {
        waitForFileToBeDeleted(path);
      }
    }
  }

  private static void waitForFileToBeDeleted(String path) {
    LOGGER.info("CE analysis is paused. Waiting for file to be deleted: " + path);
    File file = new File(path);
    try {
      while (file.exists()) {
        Thread.sleep(500L);
      }
      LOGGER.info("CE analysis is resumed");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("CE analysis has been interrupted");
    }
  }
}
