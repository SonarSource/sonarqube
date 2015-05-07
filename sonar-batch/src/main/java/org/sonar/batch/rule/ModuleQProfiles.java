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

import com.google.common.collect.ImmutableMap;
import org.sonar.api.BatchSide;
import org.sonar.batch.protocol.input.ProjectRepositories;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Map;

/**
 * Lists the Quality profiles enabled on the current module.
 */
@BatchSide
public class ModuleQProfiles {

  public static final String SONAR_PROFILE_PROP = "sonar.profile";
  private final Map<String, QProfile> byLanguage;

  public ModuleQProfiles(ProjectRepositories ref) {
    ImmutableMap.Builder<String, QProfile> builder = ImmutableMap.builder();

    for (org.sonar.batch.protocol.input.QProfile qProfile : ref.qProfiles()) {
      builder.put(qProfile.language(),
        new QProfile().setKey(qProfile.key()).setName(qProfile.name()).setLanguage(qProfile.language()).setRulesUpdatedAt(qProfile.rulesUpdatedAt()));
    }
    byLanguage = builder.build();
  }

  public Collection<QProfile> findAll() {
    return byLanguage.values();
  }

  @CheckForNull
  public QProfile findByLanguage(String language) {
    return byLanguage.get(language);
  }
}
