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
package org.sonar.server.source;

import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.ComputationStep;
import org.sonar.server.source.index.SourceLineIndexer;

public class IndexSourceLinesStep implements ComputationStep {

  private final SourceLineIndexer indexer;

  public IndexSourceLinesStep(SourceLineIndexer indexer) {
    this.indexer = indexer;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto analysisReportDto, ComponentDto project) {
    indexer.index();
  }

  @Override
  public String getDescription() {
    return "Put source code into search index";
  }

}
