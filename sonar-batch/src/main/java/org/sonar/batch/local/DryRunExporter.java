/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.local;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.4
 */
public class DryRunExporter implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DryRunExporter.class);

  private final DryRun dryRun;
  private final DefaultIndex sonarIndex;
  private final ProjectFileSystem projectFileSystem;
  private final Server server;

  public DryRunExporter(DryRun dryRun, DefaultIndex sonarIndex, ProjectFileSystem projectFileSystem, Server server) {
    this.dryRun = dryRun;
    this.sonarIndex = sonarIndex;
    this.projectFileSystem = projectFileSystem;
    this.server = server;
  }

  public void execute(SensorContext context) {
    if (!dryRun.isEnabled()) {
      return;
    }

    exportResults(sonarIndex.getResources());
  }

  private void exportResults(Collection<Resource> resources) {
    File exportFile = new File(projectFileSystem.getSonarWorkingDirectory(), dryRun.getExportPath());

    LOG.info("Exporting DryRun results to " + exportFile.getAbsolutePath());
    Writer output = null;
    try {
      output = new BufferedWriter(new FileWriter(exportFile));
      writeJson(resources, output);
      output.flush();
    } catch (IOException e) {
      throw new SonarException("Unable to write DryRun results in file " + exportFile.getAbsolutePath());
    } finally {
      Closeables.closeQuietly(output);
    }
  }

  @VisibleForTesting
  void writeJson(Collection<Resource> resources, Writer output) {
    JsonWriter json = null;
    try {
      json = new JsonWriter(output);
      json.setSerializeNulls(false);

      json.beginObject()
          .name("version").value(server.getVersion())
          .name("violations_per_resource")
          .beginObject();

      for (Resource resource : resources) {
        List<Violation> violations = getViolations(resource);
        if (violations.isEmpty()) {
          continue;
        }

        json.name(resource.getKey())
            .beginArray();

        for (Violation violation : violations) {
          json.beginObject()
              .name("line").value(violation.getLineId())
              .name("message").value(violation.getMessage())
              .name("severity").value(violation.getSeverity().name())
              .name("rule_key").value(violation.getRule().getKey())
              .name("rule_name").value(violation.getRule().getName())
              .endObject();
        }

        json.endArray();
      }

      json.endObject()
          .endObject();
    } catch (IOException e) {
      throw new SonarException("Unable to export results", e);
    } finally {
      Closeables.closeQuietly(json);
    }
  }

  @VisibleForTesting
  List<Violation> getViolations(Resource resource) {
    return sonarIndex.getViolations(resource);
  }
}
