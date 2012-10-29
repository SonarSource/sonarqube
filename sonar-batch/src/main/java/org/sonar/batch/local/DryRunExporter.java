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
import com.google.common.base.Function;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.4
 */
public class DryRunExporter implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DryRunExporter.class);

  private final DryRun dryRun;
  private final DefaultIndex sonarIndex;

  public DryRunExporter(DryRun dryRun, DefaultIndex sonarIndex) {
    this.dryRun = dryRun;
    this.sonarIndex = sonarIndex;
  }

  public void execute(SensorContext context) {
    if (!dryRun.isEnabled()) {
      return;
    }

    LOG.info("Exporting DryRun results");

    String json = getResultsAsJson(sonarIndex.getResources());
    System.out.println(json);
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
        for (Violation violation : getViolations(resource)) {
          gson.toJson(new ViolationToMap().apply(violation), writer);
        }
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

  static class ViolationToMap implements Function<Violation, JsonElement> {
    public JsonElement apply(@Nullable Violation violation) {
      JsonObject json = new JsonObject();
      if (violation != null) {
        json.addProperty("resource", violation.getResource().getKey());
        json.addProperty("line", violation.getLineId());
        json.addProperty("message", violation.getMessage());
      }
      return json;
    }
  }
}
