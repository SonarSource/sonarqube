/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

/**
 * Maps the internal, DB-stored name of the base "Sonar way" built-in quality profile — the one declared directly by
 * analyzer plugins, which can't be renamed at the source — to the brand name shown to users, and back. The derived
 * variants ({@code SonarWayCoreProfileDefinition}, {@code SonarWayExtendedProfileDefinition}) don't need an entry
 * here: since they aren't declared by analyzers, they were renamed directly at the source instead.
 * <p>
 * Storage, plugin registration and default-profile selection ({@code RegisterQualityProfiles},
 * {@code BuiltInQProfileRepositoryImpl}) keep matching on the internal name — nothing there is aware this mapping
 * exists. Only the boundary where a name is rendered to, or read from, a human (WS responses, {@code
 * QProfileReference}, startup logs) goes through this class.
 */
public final class QualityProfileDisplayNames {

  private static final Map<String, String> DISPLAY_NAME_BY_INTERNAL_NAME = Map.of(
    "Sonar way", "Sonar way comprehensive");

  private static final Map<String, String> INTERNAL_NAME_BY_DISPLAY_NAME = DISPLAY_NAME_BY_INTERNAL_NAME.entrySet().stream()
    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private QualityProfileDisplayNames() {
    // utility class
  }

  /**
   * @return the name to show to users for the given internal/stored profile name, or {@code internalName} unchanged
   * if it isn't one of the mapped "Sonar way" names.
   */
  @CheckForNull
  public static String toDisplayName(@CheckForNull String internalName) {
    if (internalName == null) {
      return null;
    }
    return DISPLAY_NAME_BY_INTERNAL_NAME.getOrDefault(internalName, internalName);
  }

  /**
   * @return the internal/stored profile name for the given user-facing display name, or {@code displayName}
   * unchanged if it isn't one of the mapped display names. Used to interpret a profile name coming from a client
   * (e.g. a WS request parameter) back into the name actually stored in the database.
   */
  @CheckForNull
  public static String toInternalName(@CheckForNull String displayName) {
    if (displayName == null) {
      return null;
    }
    return INTERNAL_NAME_BY_DISPLAY_NAME.getOrDefault(displayName, displayName);
  }
}
