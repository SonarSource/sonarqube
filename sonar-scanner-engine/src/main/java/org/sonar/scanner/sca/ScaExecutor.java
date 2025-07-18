/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.sca;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.featureflags.FeatureFlagsRepository;

/**
 * The ScaExecutor class is the main entrypoint for generating manifest dependency
 * data during a Sonar scan and passing that data in the report so that it can
 * be analyzed further by SQ server.
 */
public class ScaExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(ScaExecutor.class);
  private static final String SCA_FEATURE_NAME = "sca";

  private final CliCacheService cliCacheService;
  private final CliService cliService;
  private final ReportPublisher reportPublisher;
  private final FeatureFlagsRepository featureFlagsRepository;
  private final DefaultConfiguration configuration;

  public ScaExecutor(CliCacheService cliCacheService, CliService cliService, ReportPublisher reportPublisher, FeatureFlagsRepository featureFlagsRepository,
    DefaultConfiguration configuration) {
    this.cliCacheService = cliCacheService;
    this.cliService = cliService;
    this.reportPublisher = reportPublisher;
    this.featureFlagsRepository = featureFlagsRepository;
    this.configuration = configuration;
  }

  public void execute(DefaultInputModule root) {
    // Global feature flag
    if (!featureFlagsRepository.isEnabled(SCA_FEATURE_NAME)) {
      LOG.info("Dependency analysis skipped");
      return;
    }

    // Project or scanner level feature flag
    if (!configuration.getBoolean("sonar.sca.enabled").orElse(true)) {
      LOG.info("Dependency analysis disabled for this project");
      return;
    }

    var stopwatch = new StopWatch();
    stopwatch.start();
    LOG.info("Checking for latest CLI");
    File cliFile = cliCacheService.cacheCli();

    LOG.info("Collecting manifests for the dependency analysis...");
    if (cliFile.exists()) {
      try {
        File generatedZip = cliService.generateManifestsArchive(root, cliFile, configuration);
        LOG.debug("Zip ready for report: {}", generatedZip);
        reportPublisher.getWriter().writeScaFile(generatedZip);
        LOG.debug("Manifest zip written to report");
      } catch (IOException | IllegalStateException e) {
        LOG.error("Error gathering manifests", e);
      } finally {
        stopwatch.stop();
        if (LOG.isInfoEnabled()) {
          LOG.info("Load SCA project dependencies (done) | time={}ms", stopwatch.getTime(TimeUnit.MILLISECONDS));
        }
      }
    }
  }
}
