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

package org.sonar.server.computation.step;

import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

public class InvalidatePreviewCacheStep implements ComputationStep {
  private final PropertiesDao propertiesDao;

  public InvalidatePreviewCacheStep(PropertiesDao propertiesDao) {
    this.propertiesDao = propertiesDao;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report, ComponentDto project) {
    propertiesDao.setProperty(newProjectPreviewCacheProperty(project), session);
  }

  private PropertyDto newProjectPreviewCacheProperty(ComponentDto project) {
    return new PropertyDto()
      .setKey(PreviewCache.SONAR_PREVIEW_CACHE_LAST_UPDATE_KEY)
      .setResourceId(project.getId())
      .setValue(String.valueOf(System.currentTimeMillis()));
  }

  @Override
  public String getDescription() {
    return "Invalidate preview cache";
  }
}
