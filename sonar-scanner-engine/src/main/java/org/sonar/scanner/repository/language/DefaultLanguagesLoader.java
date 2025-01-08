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
package org.sonar.scanner.repository.language;

import com.google.gson.Gson;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonarqube.ws.client.GetRequest;

public class DefaultLanguagesLoader implements LanguagesLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultLanguagesLoader.class);
  private static final String LANGUAGES_WS_URL = "/api/languages/list";
  private static final Map<String, String> PROPERTY_FRAGMENT_MAP = Map.of(
    "js", "javascript",
    "ts", "typescript",
    "py", "python",
    "web", "html");

  private final DefaultScannerWsClient wsClient;

  private final Configuration properties;

  public DefaultLanguagesLoader(DefaultScannerWsClient wsClient, Configuration properties) {
    this.wsClient = wsClient;
    this.properties = properties;
  }

  @Override
  public Map<String, Language> load() {
    GetRequest getRequest = new GetRequest(LANGUAGES_WS_URL);
    LanguagesWSResponse response;
    try (Reader reader = wsClient.call(getRequest).contentReader()) {
      response = new Gson().fromJson(reader, LanguagesWSResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to parse response of " + LANGUAGES_WS_URL, e);
    }

    return response.languages.stream()
      .map(this::populateFileSuffixesAndPatterns)
      .collect(Collectors.toMap(Language::key, Function.identity()));

  }

  private Language populateFileSuffixesAndPatterns(SupportedLanguageDto lang) {
    lang.setFileSuffixes(getFileSuffixes(lang.getKey()));
    lang.setFilenamePatterns(getFilenamePatterns(lang.getKey()));
    if (lang.filenamePatterns() == null && lang.getFileSuffixes() == null) {
      LOG.debug("Language '{}' cannot be detected as it has neither suffixes nor patterns.", lang.getName());
    }
    return new Language(lang);
  }

  private String[] getFileSuffixes(String languageKey) {
    return getPropertyForLanguage("sonar.%s.file.suffixes", languageKey);
  }

  private String[] getFilenamePatterns(String languageKey) {
    return getPropertyForLanguage("sonar.%s.file.patterns", languageKey);
  }

  private String[] getPropertyForLanguage(String propertyPattern, String languageKey) {
    String propName = String.format(propertyPattern, PROPERTY_FRAGMENT_MAP.getOrDefault(languageKey, languageKey));
    return properties.getStringArray(propName);
  }

  private static class LanguagesWSResponse {
    List<SupportedLanguageDto> languages;
  }
}
