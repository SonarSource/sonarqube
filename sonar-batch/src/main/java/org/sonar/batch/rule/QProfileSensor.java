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
package org.sonar.batch.rule;

import com.google.common.collect.Lists;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.core.qualityprofile.db.QualityProfileDao;

import java.util.List;

/**
 * Stores which Quality profiles have been used on the current module.
 */
public class QProfileSensor implements Sensor {

  private final ModuleQProfiles moduleQProfiles;
  private final FileSystem fs;
  private final QualityProfileDao dao;

  public QProfileSensor(ModuleQProfiles moduleQProfiles, FileSystem fs, QualityProfileDao dao) {
    this.moduleQProfiles = moduleQProfiles;
    this.fs = fs;
    this.dao = dao;
  }

  public boolean shouldExecuteOnProject(Project project) {
    // Should be only executed on leaf modules
    return project.getModules().isEmpty();
  }

  public void analyse(Project project, SensorContext context) {
    List<ModuleQProfiles.QProfile> profiles = Lists.newArrayList();
    for (String language : fs.languages()) {
      ModuleQProfiles.QProfile qProfile = moduleQProfiles.findByLanguage(language);
      if (qProfile != null) {
        dao.updateUsedColumn(qProfile.id(), true);
        profiles.add(qProfile);
      }
    }
    if (profiles.size() > 0) {
      UsedQProfiles used = UsedQProfiles.fromProfiles(profiles);
      Measure detailsMeasure = new Measure(CoreMetrics.PROFILES, used.toJSON());
      context.saveMeasure(detailsMeasure);
    }

    // For backward compatibility
    if (profiles.size() == 1) {
      ModuleQProfiles.QProfile qProfile = profiles.get(0);
      Measure measure = new Measure(CoreMetrics.PROFILE, qProfile.name()).setValue((double) qProfile.id());
      Measure measureVersion = new Measure(CoreMetrics.PROFILE_VERSION, qProfile.version().doubleValue());
      context.saveMeasure(measure);
      context.saveMeasure(measureVersion);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
