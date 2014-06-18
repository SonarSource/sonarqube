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
package org.sonar.api.batch.analyzer;

import org.sonar.api.BatchExtension;

/**
 * <p>
 * An Analyzer is invoked once during the analysis of a project. The analyzer can parse a flat file, connect to a web server... Analyzers are
 * used to add measure and issues at file level.
 * </p>
 *
 * <p>
 * For example the Cobertura Analyzer parses Cobertura report and saves the first-level of measures on files.
 * </p>
 *
 * @since 4.4
 */
public interface Analyzer extends BatchExtension {

  /**
   * Populate {@link AnalyzerDescriptor} of this analyzer.
   */
  void describe(AnalyzerDescriptor descriptor);

  /**
   * The actual analyzer code.
   */
  void analyse(AnalyzerContext context);

}
