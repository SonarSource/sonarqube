/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Collection;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.updatecenter.common.Version;

public class ExportPluginsStep implements ComputationStep {

  private final PluginRepository pluginRepository;
  private final DumpWriter dumpWriter;

  public ExportPluginsStep(PluginRepository pluginRepository, DumpWriter dumpWriter) {
    this.pluginRepository = pluginRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    try (StreamWriter<ProjectDump.Plugin> writer = dumpWriter.newStreamWriter(DumpElement.PLUGINS)) {

      Collection<PluginInfo> plugins = pluginRepository.getPluginInfos();
      for (PluginInfo plugin : plugins) {
        ProjectDump.Plugin.Builder builder = ProjectDump.Plugin.newBuilder();
        writer.write(convert(plugin, builder));
      }
      LoggerFactory.getLogger(getClass()).debug("{} plugins exported", plugins.size());
    }
  }

  private static ProjectDump.Plugin convert(PluginInfo plugin, ProjectDump.Plugin.Builder builder) {
    builder
      .clear()
      .setKey(plugin.getKey())
      .setName(StringUtils.defaultString(plugin.getName()));
    Version version = plugin.getVersion();
    if (version != null) {
      builder.setVersion(version.toString());
    }
    return builder.build();
  }

  @Override
  public String getDescription() {
    return "Export plugins";
  }
}
