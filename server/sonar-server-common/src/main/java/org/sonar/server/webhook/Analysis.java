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
package org.sonar.server.webhook;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class Analysis {
  private final String uuid;
  private final long date;
  @Nullable
  private final String revision;

  public Analysis(String uuid, long date, @Nullable String revision) {
    this.uuid = requireNonNull(uuid, "uuid must not be null");
    this.date = date;
    this.revision = revision;
  }

  public String getUuid() {
    return uuid;
  }

  public long getDate() {
    return date;
  }

  public Optional<String> getRevision() {
    return Optional.ofNullable(revision);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Analysis analysis = (Analysis) o;
    return date == analysis.date &&
      uuid.equals(analysis.uuid) &&
      Objects.equals(revision, analysis.revision);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, date, revision);
  }

  @Override
  public String toString() {
    return "Analysis{" +
      "uuid='" + uuid + '\'' +
      ", date=" + date +
      ", revision=" + revision +
      '}';
  }
}
