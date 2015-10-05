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
import java.util.Properties;
import java.util.TreeSet;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.platform.PluginInfo;

@BatchSide
public class AnalysisContextReportPublisher {

  private static final Logger LOG = Loggers.get(AnalysisContextReportPublisher.class);

  private static final String ENV_PROP_PREFIX = "env.";
  private static final String SONAR_PROP_PREFIX = "sonar.";
  private final BatchPluginRepository pluginRepo;
  private final AnalysisMode mode;
  private final System2 system;

  private BatchReportWriter writer;

  public AnalysisContextReportPublisher(AnalysisMode mode, BatchPluginRepository pluginRepo, System2 system) {
    this.mode = mode;
    this.pluginRepo = pluginRepo;
    this.system = system;
  }

  public void init(BatchReportWriter writer) {
    if (mode.isIssues()) {
      return;
    }
    this.writer = writer;
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8)) {
      if (LOG.isDebugEnabled()) {
        writeEnvVariables(fileWriter);
        writeSystemProps(fileWriter);
      }
      writePlugins(fileWriter);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  private void writePlugins(BufferedWriter fileWriter) throws IOException {
    fileWriter.write("SonarQube plugins:\n");
    for (PluginInfo p : pluginRepo.getPluginInfos()) {
      fileWriter.append(String.format("  - %s %s (%s)", p.getName(), p.getVersion(), p.getKey())).append('\n');
    }
  }

  private void writeSystemProps(BufferedWriter fileWriter) throws IOException {
    fileWriter.write("System properties:\n");
    Properties sysProps = system.properties();
    for (String prop : new TreeSet<String>(sysProps.stringPropertyNames())) {
      if (prop.startsWith(SONAR_PROP_PREFIX)) {
        continue;
      }
      fileWriter.append(String.format("  - %s=%s", prop, sysProps.getProperty(prop))).append('\n');
    }
  }

  private void writeEnvVariables(BufferedWriter fileWriter) throws IOException {
    fileWriter.append("Environment variables:\n");
    Map<String, String> envVariables = system.envVariables();
    for (String env : new TreeSet<String>(envVariables.keySet())) {
      fileWriter.append(String.format("  - %s=%s", env, envVariables.get(env))).append('\n');
    }
  }

  public void dumpSettings(ProjectDefinition moduleDefinition, Settings settings) {
    if (mode.isIssues()) {
      return;
    }
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
      fileWriter.append(String.format("Settings for module: %s", moduleDefinition.getKey())).append('\n');
      Map<String, String> moduleSettings = settings.getProperties();
      for (String prop : new TreeSet<String>(moduleSettings.keySet())) {
        if (isSystemProp(prop) || isEnvVariable(prop) || !isSqProp(prop)) {
          continue;
        }
        fileWriter.append(String.format("  - %s=%s", prop, sensitive(prop) ? "******" : moduleSettings.get(prop))).append('\n');
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write analysis log", e);
    }
  }

  private static boolean isSqProp(String propKey) {
    return propKey.startsWith(SONAR_PROP_PREFIX);
  }

  private boolean isSystemProp(String propKey) {
    return system.properties().containsKey(propKey) && !propKey.startsWith(SONAR_PROP_PREFIX);
  }

  private boolean isEnvVariable(String propKey) {
    return propKey.startsWith(ENV_PROP_PREFIX) && system.envVariables().containsKey(propKey.substring(ENV_PROP_PREFIX.length()));
  }

  private static boolean sensitive(String key) {
    return key.contains(".password") || key.contains(".secured");
  }
}
