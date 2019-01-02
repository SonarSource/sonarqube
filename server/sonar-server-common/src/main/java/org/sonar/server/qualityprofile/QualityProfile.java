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
package org.sonar.server.qualityprofile;

import com.google.common.base.MoreObjects;
import java.util.Date;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * Represents the JSON object an array of which is stored in the value of the
 * {@link org.sonar.api.measures.CoreMetrics#QUALITY_PROFILES} measures.
 */
@Immutable
public class QualityProfile {
  private final String qpKey;
  private final String qpName;
  private final String languageKey;
  private final Date rulesUpdatedAt;

  public QualityProfile(String qpKey, String qpName, String languageKey, Date rulesUpdatedAt) {
    this.qpKey = requireNonNull(qpKey);
    this.qpName = requireNonNull(qpName);
    this.languageKey = requireNonNull(languageKey);
    this.rulesUpdatedAt = requireNonNull(rulesUpdatedAt);
  }

  public String getQpKey() {
    return qpKey;
  }

  public String getQpName() {
    return qpName;
  }

  public String getLanguageKey() {
    return languageKey;
  }

  public Date getRulesUpdatedAt() {
    return new Date(rulesUpdatedAt.getTime());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QualityProfile qProfile = (QualityProfile) o;
    return qpKey.equals(qProfile.qpKey);
  }

  @Override
  public int hashCode() {
    return qpKey.hashCode();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("key", qpKey)
      .add("name", qpName)
      .add("language", languageKey)
      .add("rulesUpdatedAt", rulesUpdatedAt.getTime())
      .toString();
  }
}
