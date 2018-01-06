/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

/**
 * Reference to a Quality profile as defined by requests to web services api/qualityprofiles.
 * The two exclusive options to reference a profile are:
 * <ul>
 *   <li>by its id (to be deprecated)</li>
 *   <li>by the tuple {organizationKey, language, name}</li>
 * </ul>
 */
public class QProfileReference {

  private enum Type {
    KEY, NAME
  }

  private final Type type;
  private final String key;
  private final String organizationKey;
  private final String language;
  private final String name;

  private QProfileReference(Type type, @Nullable String key, @Nullable String organizationKey, @Nullable String language, @Nullable String name) {
    this.type = type;
    if (type == Type.KEY) {
      this.key = requireNonNull(key);
      this.organizationKey = null;
      this.language = null;
      this.name = null;
    } else {
      this.key = null;
      this.organizationKey = organizationKey;
      this.language = requireNonNull(language);
      this.name = requireNonNull(name);
    }
  }

  /**
   * @return {@code true} if key is defined and {@link #getKey()} can be called. If {@code false}, then
   *   the couple {language, name} is defined and the methods {@link #getLanguage()}/{@link #getName()}
   *   can be called.
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
   * @return key of organization. It is empty when {@link #hasKey()} is {@code true} or if {@link #hasKey()} is
   * {@code false} and the default organization must be used.
   */
  public Optional<String> getOrganizationKey() {
    checkState(type == Type.NAME, "Organization is not defined. Please call hasKey().");
    return Optional.ofNullable(organizationKey);
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
    if (key != null ? !key.equals(that.key) : (that.key != null)) {
      return false;
    }
    if (organizationKey != null ? !organizationKey.equals(that.organizationKey) : (that.organizationKey != null)) {
      return false;
    }
    if (language != null ? !language.equals(that.language) : (that.language != null)) {
      return false;
    }
    return name != null ? name.equals(that.name) : (that.name == null);

  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (organizationKey != null ? organizationKey.hashCode() : 0);
    result = 31 * result + (language != null ? language.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  public static QProfileReference from(Request request) {
    String key = request.param(PARAM_KEY);
    String organizationKey = request.param(PARAM_ORGANIZATION);
    String lang = request.param(PARAM_LANGUAGE);
    String name = request.param(PARAM_QUALITY_PROFILE);
    return from(key, organizationKey, lang, name);
  }

  public static QProfileReference from(@Nullable String key, @Nullable String organizationKey, @Nullable String lang, @Nullable String name) {
    if (key != null) {
      checkArgument(isEmpty(organizationKey) && isEmpty(lang) && isEmpty(name), "When providing a quality profile key, neither of organization/language/name must be set");
      return fromKey(key);
    }
    checkArgument(!isEmpty(lang) && !isEmpty(name), "If no quality profile key is specified, language and name must be set");
    return fromName(organizationKey, lang, name);
  }

  public static QProfileReference fromKey(String key) {
    return new QProfileReference(Type.KEY, key, null, null, null);
  }

  public static QProfileReference fromName(@Nullable String organizationKey, String lang, String name) {
    return new QProfileReference(Type.NAME, null, organizationKey, requireNonNull(lang), requireNonNull(name));
  }

  public static void defineParams(WebService.NewAction action, Languages languages) {
    action.createParam(PARAM_KEY)
      .setDescription("Quality profile key")
      .setDeprecatedKey("profileKey", "6.5")
      .setDeprecatedSince("6.6")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_QUALITY_PROFILE)
      .setDescription("Quality profile name. If this parameter is set, '%s' must not be set and '%s' must be set to disambiguate.", PARAM_KEY, PARAM_LANGUAGE)
      .setDeprecatedKey("profileName", "6.6")
      .setExampleValue("Sonar way");

    action.createParam(PARAM_LANGUAGE)
      .setDescription("Quality profile language. If this parameter is set, '%s' must not be set and '%s' must be set to disambiguate.", PARAM_KEY, PARAM_LANGUAGE)
      .setPossibleValues(Arrays.stream(languages.all()).map(Language::getKey).collect(MoreCollectors.toSet()));
  }
}
