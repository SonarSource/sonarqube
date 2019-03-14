/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.rule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.utils.DateUtils;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

/**
 * Lists the Quality profiles enabled on the current project.
 */
@Immutable
public class QualityProfiles {

  private final Map<String, QProfile> byLanguage;

  public QualityProfiles(Collection<QualityProfile> profiles) {
    Map<String, QProfile> map = new HashMap<>(profiles.size());

    for (QualityProfile qProfile : profiles) {
      map.put(qProfile.getLanguage(),
        new QProfile.Builder()
          .setKey(qProfile.getKey())
          .setName(qProfile.getName())
          .setLanguage(qProfile.getLanguage())
          .setRulesUpdatedAt(DateUtils.parseDateTime(qProfile.getRulesUpdatedAt())).build());
    }
    byLanguage = Collections.unmodifiableMap(map);
  }

  public Collection<QProfile> findAll() {
    return byLanguage.values();
  }

  @CheckForNull
  public QProfile findByLanguage(String language) {
    return byLanguage.get(language);
  }
}
