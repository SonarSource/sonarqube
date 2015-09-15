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
package org.sonar.batch.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.platform.PluginInfo;

@BatchSide
public class AnalysisContextReportPublisher {

  private final BatchPluginRepository pluginRepo;
  private final AnalysisMode mode;

  private BatchReportWriter writer;

  public AnalysisContextReportPublisher(AnalysisMode mode, BatchPluginRepository pluginRepo) {
    this.mode = mode;
    this.pluginRepo = pluginRepo;
  }

  public void init(BatchReportWriter writer) {
    if (mode.isIssues()) {
      return;
    }
    this.writer = writer;
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8)) {
      fileWriter.append("Environement variables:\n");
      for (Map.Entry<String, String> env : System.getenv().entrySet()) {
        fileWriter.append(String.format("  - %s=%s", env.getKey(), env.getValue())).append('\n');
      }
      fileWriter.write("System properties:\n");
      for (Map.Entry<Object, Object> env : System.getProperties().entrySet()) {
        fileWriter.append(String.format("  - %s=%s", env.getKey(), env.getValue())).append('\n');
      }
      fileWriter.write("SonarQube plugins:\n");
      for (PluginInfo p : pluginRepo.getPluginInfos()) {
        fileWriter.append(String.format("  - %s %s (%s)", p.getName(), p.getVersion(), p.getKey())).append('\n');
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  public void dumpSettings(ProjectDefinition moduleDefinition, Settings settings) {
    if (mode.isIssues()) {
      return;
    }
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      fileWriter.append(String.format("Settings for module: %s", moduleDefinition.getKey())).append('\n');
      for (Map.Entry<String, String> prop : settings.getProperties().entrySet()) {
        fileWriter.append(String.format("  - %s=%s", prop.getKey(), sensitive(prop.getKey()) ? "******" : prop.getValue())).append('\n');
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  private static boolean sensitive(String key) {
    return key.contains(".password") || key.contains(".secured");
  }
}
