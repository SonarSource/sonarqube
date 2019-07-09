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
package org.sonar.scanner.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.bootstrap.GlobalServerSettings;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.ProjectServerSettings;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static java.util.stream.Collectors.toList;

public class AnalysisContextReportPublisher {

  private static final String KEY_VALUE_FORMAT = "  - %s=%s";

  private static final String ENV_PROP_PREFIX = "env.";
  private static final String SONAR_PROP_PREFIX = "sonar.";
  private static final int MAX_WIDTH = 1000;
  private final ScannerPluginRepository pluginRepo;
  private final ProjectServerSettings projectServerSettings;
  private final System2 system;
  private final GlobalServerSettings globalServerSettings;
  private final InputModuleHierarchy hierarchy;
  private final InputComponentStore store;

  public AnalysisContextReportPublisher(ProjectServerSettings projectServerSettings, ScannerPluginRepository pluginRepo, System2 system,
    GlobalServerSettings globalServerSettings, InputModuleHierarchy hierarchy, InputComponentStore store) {
    this.projectServerSettings = projectServerSettings;
    this.pluginRepo = pluginRepo;
    this.system = system;
    this.globalServerSettings = globalServerSettings;
    this.hierarchy = hierarchy;
    this.store = store;
  }

  public void init(ScannerReportWriter writer) {
    File analysisLog = writer.getFileStructure().analysisLog();
    try (BufferedWriter fileWriter = Files.newBufferedWriter(analysisLog.toPath(), StandardCharsets.UTF_8)) {
      writePlugins(fileWriter);
      writeGlobalSettings(fileWriter);
      writeProjectSettings(fileWriter);
      writeModulesSettings(fileWriter);
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

  private void writeGlobalSettings(BufferedWriter fileWriter) throws IOException {
    fileWriter.append("Global server settings:\n");
    Map<String, String> props = globalServerSettings.properties();
    for (String prop : new TreeSet<>(props.keySet())) {
      dumpPropIfNotSensitive(fileWriter, prop, props.get(prop));
    }
  }

  private void writeProjectSettings(BufferedWriter fileWriter) throws IOException {
    fileWriter.append("Project server settings:\n");
    Map<String, String> props = projectServerSettings.properties();
    for (String prop : new TreeSet<>(props.keySet())) {
      dumpPropIfNotSensitive(fileWriter, prop, props.get(prop));
    }
    fileWriter.append("Project scanner properties:\n");
    writeScannerProps(fileWriter, hierarchy.root().properties());
  }

  private void writeModulesSettings(BufferedWriter fileWriter) throws IOException {
    for (DefaultInputModule module : store.allModules()) {
      if (module.equals(hierarchy.root())) {
        continue;
      }
      Map<String, String> moduleSpecificProps = collectModuleSpecificProps(module);
      fileWriter.append(String.format("Scanner properties of module: %s", module.key())).append('\n');
      writeScannerProps(fileWriter, moduleSpecificProps);
    }
  }

  private void writeScannerProps(BufferedWriter fileWriter, Map<String, String> props) throws IOException {
    for (Map.Entry<String, String> prop : props.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).collect(toList())) {
      if (isSystemProp(prop.getKey()) || isEnvVariable(prop.getKey()) || !isSqProp(prop.getKey())) {
        continue;
      }
      dumpPropIfNotSensitive(fileWriter, prop.getKey(), prop.getValue());
    }
  }

  private static void dumpPropIfNotSensitive(BufferedWriter fileWriter, String prop, String value) throws IOException {
    fileWriter.append(String.format(KEY_VALUE_FORMAT, prop, isSensitiveProperty(prop) ? "******" : StringUtils.abbreviate(value, MAX_WIDTH))).append('\n');
  }

  /**
   * Only keep props that are not in parent
   */
  private Map<String, String> collectModuleSpecificProps(DefaultInputModule module) {
    Map<String, String> moduleSpecificProps = new HashMap<>();
    AbstractProjectOrModule parent = hierarchy.parent(module);
    if (parent == null) {
      moduleSpecificProps.putAll(module.properties());
    } else {
      Map<String, String> parentProps = parent.properties();
      for (Map.Entry<String, String> entry : module.properties().entrySet()) {
        if (!parentProps.containsKey(entry.getKey()) || !parentProps.get(entry.getKey()).equals(entry.getValue())) {
          moduleSpecificProps.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return moduleSpecificProps;
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

  private static boolean isSensitiveProperty(String key) {
    return key.equals(CoreProperties.LOGIN) || key.contains(".password") || key.contains(".secured") || key.contains(".token");
  }
}
