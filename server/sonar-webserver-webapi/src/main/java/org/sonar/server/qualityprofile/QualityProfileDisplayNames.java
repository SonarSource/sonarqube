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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.server.qualityprofile.builtin.sonarwayvariants.SonarWayCoreProfileDefinition;
import org.sonar.server.qualityprofile.builtin.sonarwayvariants.SonarWayExtendedProfileDefinition;

/**
 * Maps the internal, DB-stored name of the base "Sonar way" built-in quality profile — the one declared directly by
 * analyzer plugins, which can't be renamed at the source — to the brand name shown to users, and back. The derived
 * variants ({@code SonarWayCoreProfileDefinition}, {@code SonarWayExtendedProfileDefinition}) don't need a mapping
 * entry: since they aren't declared by analyzers, they were renamed directly at the source instead. They're still
 * covered by {@link #isReservedName(String)}, though — see {@link #RESERVED_NAMES}.
 * <p>
 * Storage, plugin registration and default-profile selection ({@code RegisterQualityProfiles},
 * {@code BuiltInQProfileRepositoryImpl}) keep matching on the internal name — nothing there is aware this mapping
 * exists. Only the boundary where a name is rendered to, or read from, a human (WS responses, {@code
 * QProfileReference}, startup logs) goes through this class.
 * <p>
 * The mapping only makes sense for the actual built-in profile: callers must pass {@code isBuiltIn} so a custom
 * profile that happens to be named "Sonar way" isn't display-renamed to "Sonar way comprehensive", and
 * {@link #isReservedName(String)} lets name-creation endpoints (create/copy/rename) reject that same name (in
 * either its internal or display form) so a custom profile can never collide with it in the first place — without
 * that, a request addressing a custom profile literally named "Sonar way comprehensive" would silently resolve to
 * the built-in "Sonar way" profile instead.
 */
public final class QualityProfileDisplayNames {

  /**
   * The internal, DB-stored name analyzers declare for the base built-in profile. Exposed so other classes that
   * need to refer to it (e.g. {@link RegisterQualityProfiles#DEFAULT_PROFILE_NAME}, {@code SearchAction}'s pinned
   * sort order) reference this constant instead of repeating the literal.
   */
  public static final String SONAR_WAY_INTERNAL_NAME = "Sonar way";

  private static final Map<String, String> DISPLAY_NAME_BY_INTERNAL_NAME = Map.of(
    SONAR_WAY_INTERNAL_NAME, "Sonar way comprehensive");

  private static final Map<String, String> INTERNAL_NAME_BY_DISPLAY_NAME = DISPLAY_NAME_BY_INTERNAL_NAME.entrySet().stream()
    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  /**
   * Names a custom profile must never take: the base profile's internal/display names, plus the two variants'
   * names. The variants aren't in {@link #DISPLAY_NAME_BY_INTERNAL_NAME} (they need no translation, since they're
   * already stored under their user-facing name), but they're just as real a built-in name as "Sonar way" is, so a
   * custom profile colliding with either — for a language that doesn't have that built-in variant yet, but might
   * once an analyzer adds it — is reserved here too.
   */
  private static final Set<String> RESERVED_NAMES = Stream.of(
    Stream.of(SonarWayCoreProfileDefinition.NAME, SonarWayExtendedProfileDefinition.NAME),
    DISPLAY_NAME_BY_INTERNAL_NAME.keySet().stream(),
    DISPLAY_NAME_BY_INTERNAL_NAME.values().stream())
    .flatMap(Function.identity())
    .collect(Collectors.toUnmodifiableSet());

  private QualityProfileDisplayNames() {
    // utility class
  }

  /**
   * @return the name to show to users for the given internal/stored profile name, or {@code internalName} unchanged
   * if {@code isBuiltIn} is {@code false} or the name isn't one of the mapped "Sonar way" names. {@code isBuiltIn}
   * matters because a custom (non-built-in) profile could otherwise be named exactly "Sonar way" and get rendered
   * under the built-in's display name.
   */
  @CheckForNull
  public static String toDisplayName(@CheckForNull String internalName, boolean isBuiltIn) {
    if (internalName == null || !isBuiltIn) {
      return internalName;
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

  /**
   * @return {@code true} if {@code name} is one of the internal or display names this class maps, i.e. a custom
   * profile must not be allowed to take this name — see {@link CreateAction}, {@link CopyAction}, {@link RenameAction}.
   */
  public static boolean isReservedName(String name) {
    return RESERVED_NAMES.contains(name);
  }
}
