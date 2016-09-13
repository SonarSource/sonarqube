/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import javax.annotation.Nullable;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.util.LanguageParamUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Reference to a Quality profile. The two exclusive options to reference a profile are:
 * <ul>
 *   <li>by its key</li>
 *   <li>by the couple {language, name}</li>
 * </ul>
 */
public class QProfileRef {

  public static final String PARAM_LANGUAGE = "language";
  public static final String PARAM_PROFILE_NAME = "profileName";
  public static final String PARAM_PROFILE_KEY = "profileKey";

  private final String key;
  private final String language;
  private final String name;

  private QProfileRef(@Nullable String key, @Nullable String language, @Nullable String name) {
    this.key = key;
    this.language = language;
    this.name = name;
  }

  /**
   * @return {@code true} if key is defined and {@link #getKey()} can be called. If {@code false}, then
   *   the couple {language, name} is defined and the methods {@link #getLanguage()}/{@link #getName()}
   *   can be called.
   */
  public boolean hasKey() {
    return this.key != null;
  }

  /**
   * @return non-null key
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code true}
   */
  public String getKey() {
    checkState(key != null, "Key is not present. Please call hasKey().");
    return key;
  }

  /**
   * @return non-null language
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code false}
   */
  public String getLanguage() {
    checkState(language != null, "Language is not present. Please call hasKey().");
    return language;
  }

  /**
   * @return non-null name
   * @throws IllegalStateException if {@link #hasKey()} does not return {@code false}
   */
  public String getName() {
    checkState(name != null, "Name is not present. Please call hasKey().");
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
    QProfileRef that = (QProfileRef) o;
    if (key != null ? !key.equals(that.key) : (that.key != null)) {
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
    result = 31 * result + (language != null ? language.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  public static QProfileRef from(Request request) {
    String key = request.param(PARAM_PROFILE_KEY);
    String lang = request.param(PARAM_LANGUAGE);
    String name = request.param(PARAM_PROFILE_NAME);
    return from(key, lang, name);
  }

  public static QProfileRef from(@Nullable String key, @Nullable String lang, @Nullable String name) {
    if (key != null) {
      checkArgument(isEmpty(lang) && isEmpty(name), "Either key or couple language/name must be set");
      return fromKey(key);
    }
    checkArgument(!isEmpty(lang) && !isEmpty(name), "Both profile language and name must be set");
    return fromName(lang, name);
  }

  public static QProfileRef fromKey(String key) {
    return new QProfileRef(requireNonNull(key), null, null);
  }

  public static QProfileRef fromName(String lang, String name) {
    return new QProfileRef(null, requireNonNull(lang), requireNonNull(name));
  }

  public static void defineParams(WebService.NewAction action, Languages languages) {
    action.createParam(PARAM_PROFILE_KEY)
      .setDescription("A quality profile key. Either this parameter, or a combination of profileName + language must be set.")
      .setExampleValue("sonar-way-java-12345");
    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("A quality profile name. If this parameter is set, profileKey must not be set and language must be set to disambiguate.")
      .setExampleValue("Sonar way");
    action.createParam(PARAM_LANGUAGE)
      .setDescription("A quality profile language. If this parameter is set, profileKey must not be set and profileName must be set to disambiguate.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setExampleValue("js");
  }
}
