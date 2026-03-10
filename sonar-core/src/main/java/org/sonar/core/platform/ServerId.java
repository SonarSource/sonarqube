/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public final class ServerId {

  public static final char SPLIT_CHARACTER = '-';
  public static final int DATABASE_ID_LENGTH = 8;
  public static final int DEPRECATED_SERVER_ID_LENGTH = 14;
  public static final int NOT_UUID_DATASET_ID_LENGTH = 15;
  public static final int UUID_DATASET_ID_LENGTH = 20;
  private static final Set<Integer> ALLOWED_LENGTHS = ImmutableSet.of(
    NOT_UUID_DATASET_ID_LENGTH + 1 + DATABASE_ID_LENGTH,
    UUID_DATASET_ID_LENGTH + 1 + DATABASE_ID_LENGTH);

  private final String databaseId;
  private final String datasetId;

  private ServerId(String databaseId, String datasetId) {
    this.databaseId = databaseId;
    this.datasetId = datasetId;
  }

  public String getDatabaseId() {
    return databaseId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public static ServerId parse(String serverId) {
    String trimmed = serverId.trim();

    int length = trimmed.length();
    checkArgument(length > 0, "serverId can't be empty");
    checkArgument(ALLOWED_LENGTHS.contains(length), "serverId does not have a supported length. Only WITH_DATABASE_ID format is supported.");

    int splitCharIndex = trimmed.indexOf(SPLIT_CHARACTER);
    checkArgument(splitCharIndex != -1, "serverId must contain database ID and dataset ID separated by '-'");
    checkArgument(splitCharIndex == DATABASE_ID_LENGTH, "Unrecognized serverId format. Parts have wrong length");
    return of(trimmed.substring(0, splitCharIndex), trimmed.substring(splitCharIndex + 1));
  }

  public static ServerId of(String databaseId, String datasetId) {
    checkArgument(databaseId != null, "databaseId is required. Deprecated formats without database ID are no longer supported.");
    int databaseIdLength = databaseId.length();
    checkArgument(databaseIdLength == DATABASE_ID_LENGTH, "Illegal databaseId length (%s)", databaseIdLength);
    int datasetIdLength = datasetId.length();
    checkArgument(datasetIdLength == NOT_UUID_DATASET_ID_LENGTH
      || datasetIdLength == UUID_DATASET_ID_LENGTH, "Illegal datasetId length (%s)", datasetIdLength);
    return new ServerId(databaseId, datasetId);
  }

  @Override
  public String toString() {
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
