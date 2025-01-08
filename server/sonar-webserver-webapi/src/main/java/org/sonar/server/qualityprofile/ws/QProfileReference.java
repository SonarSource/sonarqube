/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

/**
 * Reference to a Quality profile as defined by requests to web services api/qualityprofiles.
 * The two exclusive options to reference a profile are:
 * <ul>
 * <li>by its id (to be deprecated)</li>
 * <li>by the tuple {language, name}</li>
 * </ul>
 */
public class QProfileReference {

  private enum Type {
    KEY, NAME
  }

  private final Type type;
  private final String key;
  private final String language;
  private final String name;

  private QProfileReference(Type type, @Nullable String key, @Nullable String language, @Nullable String name) {
    this.type = type;
    if (type == Type.KEY) {
      this.key = requireNonNull(key);
      this.language = null;
      this.name = null;
    } else {
      this.key = null;
      this.language = requireNonNull(language);
      this.name = requireNonNull(name);
    }
  }

  /**
   * @return {@code true} if key is defined and {@link #getKey()} can be called. If {@code false}, then
   * the couple {language, name} is defined and the methods {@link #getLanguage()}/{@link #getName()}
   * can be called.
   */
  public boolean hasKey() {
    return type == Type.KEY;
  }

  /**
   * @return non-null key
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code true}
   */
  public String getKey() {
    checkState(key != null, "Key is not defined. Please call hasKey().");
    return key;
  }

  /**
   * @return non-null language
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code false}
   */
  public String getLanguage() {
    checkState(type == Type.NAME, "Language is not defined. Please call hasKey().");
    return language;
  }

  /**
   * @return non-null name
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code false}
   */
  public String getName() {
    checkState(type == Type.NAME, "Name is not defined. Please call hasKey().");
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileReference that = (QProfileReference) o;
    return Objects.equals(key, that.key) && Objects.equals(language, that.language) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, language, name);
  }

  public static QProfileReference fromName(Request request) {
    String lang = request.mandatoryParam(PARAM_LANGUAGE);
    String name = request.mandatoryParam(PARAM_QUALITY_PROFILE);
    return fromName(lang, name);
  }

  public static QProfileReference fromKey(String key) {
    return new QProfileReference(Type.KEY, key, null, null);
  }

  public static QProfileReference fromName(String lang, String name) {
    return new QProfileReference(Type.NAME, null, requireNonNull(lang), requireNonNull(name));
  }

  public static void defineParams(WebService.NewAction action, Languages languages) {
    action.createParam(PARAM_QUALITY_PROFILE)
      .setDescription("Quality profile name.")
      .setRequired(true)
      .setExampleValue("Sonar way");

    action.createParam(PARAM_LANGUAGE)
      .setDescription("Quality profile language.")
      .setRequired(true)
      .setPossibleValues(Arrays.stream(languages.all()).map(Language::getKey).collect(Collectors.toSet()));
  }
}
