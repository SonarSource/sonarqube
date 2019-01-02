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
package org.sonar.core.platform;

import com.google.common.collect.ImmutableSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.CoreProperties;
import org.sonar.core.util.UuidFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.platform.ServerId.Format.DEPRECATED;
import static org.sonar.core.platform.ServerId.Format.NO_DATABASE_ID;
import static org.sonar.core.platform.ServerId.Format.WITH_DATABASE_ID;

@Immutable
public final class ServerId {

  public static final char SPLIT_CHARACTER = '-';
  public static final int DATABASE_ID_LENGTH = 8;
  public static final int DEPRECATED_SERVER_ID_LENGTH = 14;
  public static final int NOT_UUID_DATASET_ID_LENGTH = 15;
  public static final int UUID_DATASET_ID_LENGTH = 20;
  private static final Set<Integer> ALLOWED_LENGTHS = ImmutableSet.of(
    DEPRECATED_SERVER_ID_LENGTH,
    NOT_UUID_DATASET_ID_LENGTH,
    NOT_UUID_DATASET_ID_LENGTH + 1 + DATABASE_ID_LENGTH,
    UUID_DATASET_ID_LENGTH,
    UUID_DATASET_ID_LENGTH + 1 + DATABASE_ID_LENGTH);

  public enum Format {
    /* server id format before 6.1 (see SONAR-6992) */
    DEPRECATED,
    /* server id format before 6.7.5 and 7.3 (see LICENSE-96) */
    NO_DATABASE_ID,
    WITH_DATABASE_ID
  }

  private final String databaseId;
  private final String datasetId;
  private final Format format;

  private ServerId(@Nullable String databaseId, String datasetId) {
    this.databaseId = databaseId;
    this.datasetId = datasetId;
    this.format = computeFormat(databaseId, datasetId);
  }

  public Optional<String> getDatabaseId() {
    return Optional.ofNullable(databaseId);
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Format getFormat() {
    return format;
  }

  private static Format computeFormat(@Nullable String databaseId, String datasetId) {
    if (databaseId != null) {
      return WITH_DATABASE_ID;
    }
    if (isDate(datasetId)) {
      return DEPRECATED;
    }
    return NO_DATABASE_ID;
  }

  public static ServerId parse(String serverId) {
    String trimmed = serverId.trim();

    int length = trimmed.length();
    checkArgument(length > 0, "serverId can't be empty");
    checkArgument(ALLOWED_LENGTHS.contains(length), "serverId does not have a supported length");
    if (length == DEPRECATED_SERVER_ID_LENGTH || length == UUID_DATASET_ID_LENGTH || length == NOT_UUID_DATASET_ID_LENGTH) {
      return new ServerId(null, trimmed);
    }

    int splitCharIndex = trimmed.indexOf(SPLIT_CHARACTER);
    if (splitCharIndex == -1) {
      return new ServerId(null, trimmed);
    }
    checkArgument(splitCharIndex == DATABASE_ID_LENGTH, "Unrecognized serverId format. Parts have wrong length");
    return of(trimmed.substring(0, splitCharIndex), trimmed.substring(splitCharIndex + 1));
  }

  public static ServerId of(@Nullable String databaseId, String datasetId) {
    if (databaseId != null) {
      int databaseIdLength = databaseId.length();
      checkArgument(databaseIdLength == DATABASE_ID_LENGTH, "Illegal databaseId length (%s)", databaseIdLength);
    }
    int datasetIdLength = datasetId.length();
    checkArgument(datasetIdLength == DEPRECATED_SERVER_ID_LENGTH
      || datasetIdLength == NOT_UUID_DATASET_ID_LENGTH
      || datasetIdLength == UUID_DATASET_ID_LENGTH, "Illegal datasetId length (%s)", datasetIdLength);
    return new ServerId(databaseId, datasetId);
  }

  public static ServerId create(UuidFactory uuidFactory) {
    return new ServerId(null, uuidFactory.create());
  }

  /**
   * Checks whether the specified value is a date according to the old format of the {@link CoreProperties#SERVER_ID}.
   */
  private static boolean isDate(String value) {
    try {
      new SimpleDateFormat("yyyyMMddHHmmss").parse(value);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    if (databaseId == null) {
      return datasetId;
    }
    return databaseId + SPLIT_CHARACTER + datasetId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerId serverId = (ServerId) o;
    return Objects.equals(databaseId, serverId.databaseId) &&
      Objects.equals(datasetId, serverId.datasetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(databaseId, datasetId);
  }
}
