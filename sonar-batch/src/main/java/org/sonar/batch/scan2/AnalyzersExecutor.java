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
package org.sonar.batch.scan2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.analyzer.Analyzer;
import org.sonar.api.batch.analyzer.AnalyzerContext;
import org.sonar.api.batch.analyzer.internal.DefaultAnalyzerDescriptor;
import org.sonar.api.platform.ComponentContainer;

import java.util.Collection;

public class AnalyzersExecutor implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyzersExecutor.class);

  private ComponentContainer container;

  public AnalyzersExecutor(ComponentContainer container) {
    this.container = container;
  }

  public void execute(AnalyzerContext context) {
    Collection<Analyzer> analyzers = container.getComponentsByType(Analyzer.class);

    for (Analyzer analyzer : analyzers) {

      DefaultAnalyzerDescriptor descriptor = new DefaultAnalyzerDescriptor();
      analyzer.describe(descriptor);

      LOG.info("Execute analyzer: " + descriptor.name());

      executeSensor(context, analyzer);
    }

  }

  private void executeSensor(AnalyzerContext context, Analyzer analyzer) {
    analyzer.analyse(context);
  }

}
