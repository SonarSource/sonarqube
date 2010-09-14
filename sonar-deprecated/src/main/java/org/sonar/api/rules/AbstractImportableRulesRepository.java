/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rules;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Deprecated
public abstract class AbstractImportableRulesRepository<LANG extends Language, MAPPER extends RulePriorityMapper<?, ?>> extends AbstractRulesRepository<LANG, MAPPER> implements ConfigurationImportable {

  public AbstractImportableRulesRepository(LANG language, MAPPER mapper) {
    super(language, mapper);
  }

  /**
   * A map a of profiles to import, The profile name as key, and the xml profile file name in the classpath
   *
   * @return
   */
  public abstract Map<String, String> getBuiltInProfiles();

  public final List<RulesProfile> getProvidedProfiles() {
    List<RulesProfile> profiles = new ArrayList<RulesProfile>();

    Map<String, String> defaultProfiles = new TreeMap<String, String>(getBuiltInProfiles());
    for (Map.Entry<String, String> entry : defaultProfiles.entrySet()) {
      profiles.add(loadProvidedProfile(entry.getKey(), getCheckResourcesBase() + entry.getValue()));
    }
    return profiles;
  }

  public final RulesProfile loadProvidedProfile(String name, String fileName) {
    InputStream input = null;
    try {
      input = getClass().getResourceAsStream(fileName);
      RulesProfile profile = new RulesProfile(name, getLanguage().getKey());
      profile.setActiveRules(importConfiguration(IOUtils.toString(input, CharEncoding.UTF_8), getInitialReferential()));
      return profile;

    } catch (IOException e) {
      throw new SonarException("Configuration file not found for the profile : " + name, e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

}
