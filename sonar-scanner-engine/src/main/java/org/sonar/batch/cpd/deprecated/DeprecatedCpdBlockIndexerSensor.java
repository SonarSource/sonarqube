/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.cpd.deprecated;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;

/**
 * Feed block index using deprecated {@link CpdMapping} extension point if not already
 * fed by another Sensor using {@link SensorContext#newCpdTokens()}. Special case for Java
 * that use a dedicated block indexer.
 * Can be removed when {@link CpdMapping} extension is removed and Java specific part moved to Java plugin.
 */
@Phase(name = Phase.Name.POST)
public class DeprecatedCpdBlockIndexerSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(DeprecatedCpdBlockIndexerSensor.class);

  private CpdBlockIndexer javaCpdBlockIndexer;
  private CpdBlockIndexer defaultCpdBlockIndexer;
  private Settings settings;
  private FileSystem fs;

  public DeprecatedCpdBlockIndexerSensor(JavaCpdBlockIndexer javaCpdBlockIndexer, DefaultCpdBlockIndexer defaultCpdBlockIndexer, Settings settings, FileSystem fs) {
    this.javaCpdBlockIndexer = javaCpdBlockIndexer;
    this.defaultCpdBlockIndexer = defaultCpdBlockIndexer;
    this.settings = settings;
    this.fs = fs;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("CPD Block Indexer");
  }

  @VisibleForTesting
  CpdBlockIndexer getBlockIndexer(String language) {
    if (javaCpdBlockIndexer.isLanguageSupported(language)) {
      return javaCpdBlockIndexer;
    }
    return defaultCpdBlockIndexer;
  }

  @VisibleForTesting
  boolean isSkipped(String language) {
    String key = "sonar.cpd." + language + ".skip";
    if (settings.hasKey(key)) {
      return settings.getBoolean(key);
    }
    return settings.getBoolean(CoreProperties.CPD_SKIP_PROPERTY);
  }

  @Override
  public void execute(SensorContext context) {
    if (settings.hasKey(CoreProperties.CPD_SKIP_PROPERTY)) {
      LOG.warn("\"sonar.cpd.skip\" property is deprecated and will be removed. Please set \"sonar.cpd.exclusions=**\" instead to disable duplication mechanism.");
    }

    for (String language : fs.languages()) {
      if (settings.hasKey("sonar.cpd." + language + ".skip")) {
        LOG
          .warn("\"sonar.cpd." + language + ".skip\" property is deprecated and will be removed. Please set \"sonar.cpd.exclusions=**\" instead to disable duplication mechanism.");
      }

      if (isSkipped(language)) {
        LOG.info("Detection of duplicated code is skipped for {}", language);
        continue;
      }

      CpdBlockIndexer blockIndexer = getBlockIndexer(language);
      if (!blockIndexer.isLanguageSupported(language)) {
        LOG.debug("Detection of duplicated code is not supported for {}", language);
        continue;
      }
      LOG.info("{} is used for {}", blockIndexer, language);
      blockIndexer.index(language);
    }
  }

}
