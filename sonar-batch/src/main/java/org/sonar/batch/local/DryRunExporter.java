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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.bootstrap.DryRun;
import org.sonar.batch.index.DefaultIndex;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    LOG.info("Exporting dry run results");

    List<Map<String, ? extends Serializable>> results = Lists.newArrayList();

    for (Resource resource : sonarIndex.getResources()) {
      List<Violation> violations = sonarIndex.getViolations(ViolationQuery.create().forResource(resource));
      for (Violation violation : violations) {
        results.add(ImmutableMap.of(
            "resource", violation.getResource().getKey(),
            "line", violation.getLineId(),
            "message", violation.getMessage()));

      }
    }

    String json = new Gson().toJson(results);
    System.out.println(json);
  }
}
