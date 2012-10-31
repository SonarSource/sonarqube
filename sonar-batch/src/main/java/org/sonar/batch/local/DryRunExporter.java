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
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 3.4
 */
public class DryRunExporter implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DryRunExporter.class);

  private final DryRun dryRun;
  private final DefaultIndex sonarIndex;
  private final ProjectFileSystem projectFileSystem;

  public DryRunExporter(DryRun dryRun, DefaultIndex sonarIndex, ProjectFileSystem projectFileSystem) {
    this.dryRun = dryRun;
    this.sonarIndex = sonarIndex;
    this.projectFileSystem = projectFileSystem;
  }

  public void execute(SensorContext context) {
    if (!dryRun.isEnabled()) {
      return;
    }

    String json = getResultsAsJson(sonarIndex.getResources());
    exportResults(json);
  }

  private void exportResults(String json) {
    File exportFile = new File(projectFileSystem.getSonarWorkingDirectory(), dryRun.getExportPath());

    LOG.info("Exporting DryRun results to " + exportFile.getAbsolutePath());
    try {
      Files.write(json, exportFile, Charsets.UTF_8);
    } catch (IOException e) {
      throw new SonarException("Unable to write DryRun results in file " + exportFile.getAbsolutePath());
    }
  }

  @VisibleForTesting
  String getResultsAsJson(Collection<Resource> resources) {
    Gson gson = new Gson();

    StringWriter output = new StringWriter();

    JsonWriter writer = null;
    try {
      writer = new JsonWriter(output);
      writer.beginArray();

      for (Resource resource : resources) {
        List<Map<String, Object>> resourceViolations = Lists.newArrayList();
        for (Violation violation : getViolations(resource)) {
          resourceViolations.add(ImmutableMap.<String, Object> of(
              "line", violation.getLineId(),
              "message", violation.getMessage(),
              "severity", violation.getSeverity().name(),
              "rule_key", violation.getRule().getKey(),
              "rule_name", violation.getRule().getName()));
        }

        Map<String, Object> obj = ImmutableMap.of(
            "resource", resource.getKey(),
            "violations", resourceViolations);

        gson.toJson(obj, Map.class, writer);
      }

      writer.endArray();
    } catch (IOException e) {
      throw new SonarException("Unable to export results", e);
    } finally {
      Closeables.closeQuietly(writer);
    }

    return output.toString();
  }

  @VisibleForTesting
  List<Violation> getViolations(Resource resource) {
    return sonarIndex.getViolations(resource);
  }
}
