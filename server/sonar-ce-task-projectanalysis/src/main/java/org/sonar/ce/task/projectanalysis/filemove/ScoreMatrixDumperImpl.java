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
package org.sonar.ce.task.projectanalysis.filemove;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.CeTask;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ScoreMatrixDumperImpl implements ScoreMatrixDumper {
  private static final Logger LOG = Loggers.get(ScoreMatrixDumperImpl.class);

  private final Configuration configuration;
  private final CeTask ceTask;

  public ScoreMatrixDumperImpl(Configuration configuration, CeTask ceTask) {
    this.configuration = configuration;
    this.ceTask = ceTask;
  }

  @Override
  public void dumpAsCsv(ScoreMatrix scoreMatrix) {
    if (configuration.getBoolean("sonar.filemove.dumpCsv").orElse(false)) {
      try {
        Path tempFile = Files.createTempFile(String.format("score-matrix-%s", ceTask.getUuid()), ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, UTF_8)) {
          writer.write(scoreMatrix.toCsv(';'));
        }
        LOG.info("File move similarity score matrix dumped as CSV: {}", tempFile);
      } catch (IOException e) {
        LOG.error("Failed to dump ScoreMatrix as CSV", e);
      }
    }
  }
}
