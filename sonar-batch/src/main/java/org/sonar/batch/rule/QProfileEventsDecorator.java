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

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.Event;
import org.sonar.api.batch.TimeMachine;
import org.sonar.api.batch.TimeMachineQuery;
import org.sonar.api.batch.rules.QProfile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

public class QProfileEventsDecorator implements Decorator {

  private final TimeMachine timeMachine;
  private final Languages languages;

  public QProfileEventsDecorator(TimeMachine timeMachine, Languages languages) {
    this.timeMachine = timeMachine;
    this.languages = languages;
  }

  @DependsUpon
  public Metric dependsUpon() {
    return CoreMetrics.QUALITY_PROFILES;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    if (!Qualifiers.isProject(resource, true)) {
      return;
    }

    // Load previous profiles
    Measure previousMeasure = getPreviousMeasure(resource, CoreMetrics.QUALITY_PROFILES);
    if (previousMeasure == null || previousMeasure.getData() == null) {
      // first analysis -> do not generate events
      return;
    }
    Map<String, QProfile> previousProfiles = UsedQProfiles.fromJson(previousMeasure.getData()).profilesByKey();

    // Load current profiles
    Measure currentMeasure = context.getMeasure(CoreMetrics.QUALITY_PROFILES);
    Map<String, QProfile> currentProfiles = UsedQProfiles.fromJson(currentMeasure.getData()).profilesByKey();

    // Detect new profiles or updated profiles
    for (QProfile profile : currentProfiles.values()) {
      QProfile previousProfile = previousProfiles.get(profile.key());
      if (previousProfile != null) {
        // TODO compare date
      } else {
        usedProfile(context, profile);
      }
    }

    // Detect profiles that are not used anymore
    for (QProfile previousProfile : previousProfiles.values()) {
      if (!currentProfiles.containsKey(previousProfile.key())) {
        stopUsedProfile(context, previousProfile);
      }
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
    return profile.name();
  }

  @CheckForNull
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
