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

import com.google.common.collect.Maps;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.rule.ModuleQProfiles.QProfile;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;

import java.util.List;
import java.util.Map;

public class QProfileEventsDecorator implements Decorator {

  private final TimeMachine timeMachine;
  private final QualityProfileDao qualityProfileDao;
  private final Languages languages;

  public QProfileEventsDecorator(TimeMachine timeMachine, QualityProfileDao qualityProfileDao, Languages languages) {
    this.timeMachine = timeMachine;
    this.qualityProfileDao = qualityProfileDao;
    this.languages = languages;
  }

  @DependsUpon
  public Metric dependsUpon() {
    return CoreMetrics.PROFILES;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!Qualifiers.isProject(resource, true)) {
      return;
    }

    // Load current profiles
    Measure profilesMeasure = context.getMeasure(CoreMetrics.PROFILES);
    UsedQProfiles currentProfiles = UsedQProfiles.fromJSON(profilesMeasure.getData());

    // Now load previous profiles
    UsedQProfiles pastProfiles;
    // First try with new metric
    Measure pastProfilesMeasure = getPreviousMeasure(resource, CoreMetrics.PROFILES);
    if (pastProfilesMeasure != null) {
      pastProfiles = UsedQProfiles.fromJSON(pastProfilesMeasure.getData());
    } else {
      // Fallback to old metric
      Measure pastProfileMeasure = getPreviousMeasure(resource, CoreMetrics.PROFILE);
      if (pastProfileMeasure == null) {
        // first analysis
        return;
      }
      int pastProfileId = pastProfileMeasure.getIntValue();
      String pastProfileName = pastProfileMeasure.getData();
      QualityProfileDto pastProfile = qualityProfileDao.selectById(pastProfileId);
      String pastProfileLanguage = "unknow";
      if (pastProfile != null) {
        pastProfileLanguage = pastProfile.getLanguage();
      }
      Measure pastProfileVersionMeasure = getPreviousMeasure(resource, CoreMetrics.PROFILE_VERSION);
      final int pastProfileVersion;
      // first analysis with versions
      if (pastProfileVersionMeasure == null) {
        pastProfileVersion = 1;
      } else {
        pastProfileVersion = pastProfileVersionMeasure.getIntValue();
      }
      pastProfiles = UsedQProfiles.fromProfiles(new ModuleQProfiles.QProfile(pastProfileId, pastProfileName, pastProfileLanguage, pastProfileVersion));
    }

    // Now create appropriate events
    Map<Integer, QProfile> pastProfilesById = Maps.newHashMap(pastProfiles.profilesById());
    for (QProfile profile : currentProfiles.profilesById().values()) {
      if (pastProfilesById.containsKey(profile.id())) {
        QProfile pastProfile = pastProfilesById.get(profile.id());
        if (pastProfile.version() < profile.version()) {
          // New version of the same QP
          usedProfile(context, profile);
        }
        pastProfilesById.remove(profile.id());
      } else {
        usedProfile(context, profile);
      }
    }
    for (QProfile profile : pastProfilesById.values()) {
      // Following profiles are no more used
      stopUsedProfile(context, profile);
    }
  }

  private void stopUsedProfile(DecoratorContext context, QProfile profile) {
    Language language = languages.get(profile.language());
    String languageName = language != null ? language.getName() : profile.language();
    context.createEvent("Stop using " + format(profile) + " (" + languageName + ")", format(profile) + " no more used for " + languageName, Event.CATEGORY_PROFILE, null);
  }

  private void usedProfile(DecoratorContext context, QProfile profile) {
    Language language = languages.get(profile.language());
    String languageName = language != null ? language.getName() : profile.language();
    context.createEvent("Use " + format(profile) + " (" + languageName + ")", format(profile) + " used for " + languageName, Event.CATEGORY_PROFILE, null);
  }

  private String format(QProfile profile) {
    return profile.name() + " version " + profile.version();
  }

  private Measure getPreviousMeasure(Resource project, Metric metric) {
    TimeMachineQuery query = new TimeMachineQuery(project)
      .setOnlyLastAnalysis(true)
      .setMetrics(metric);
    List<Measure> measures = timeMachine.getMeasures(query);
    if (measures.isEmpty()) {
      return null;
    }
    return measures.get(0);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
