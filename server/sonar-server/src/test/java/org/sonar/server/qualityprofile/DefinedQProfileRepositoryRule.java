/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.resources.Language;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkState;
import static org.sonar.core.util.stream.MoreCollectors.toList;

public class DefinedQProfileRepositoryRule extends ExternalResource implements DefinedQProfileRepository {
  private boolean initializeCalled = false;
  private Map<String, List<DefinedQProfile>> qProfilesbyLanguage = new HashMap<>();

  @Override
  protected void before() throws Throwable {
    this.initializeCalled = false;
    this.qProfilesbyLanguage.clear();
  }

  @Override
  public void initialize() {
    checkState(!initializeCalled, "initialize must be called only once");
    this.initializeCalled = true;
  }

  @Override
  public Map<String, List<DefinedQProfile>> getQProfilesByLanguage() {
    checkState(initializeCalled, "initialize must be called first");

    return ImmutableMap.copyOf(qProfilesbyLanguage);
  }

  public boolean isInitialized() {
    return initializeCalled;
  }

  public DefinedQProfileRepositoryRule set(String languageKey, DefinedQProfile first, DefinedQProfile... others) {
    qProfilesbyLanguage.put(
      languageKey,
      Stream.concat(Stream.of(first), Arrays.stream(others)).collect(toList(1 + others.length)));
    return this;
  }

  public DefinedQProfile add(Language language, String profileName) {
    return add(language, profileName, false);
  }

  public DefinedQProfile add(Language language, String profileName, boolean isDefault) {
    DefinedQProfile definedQProfile = create(language, profileName, isDefault);
    qProfilesbyLanguage.compute(language.getKey(),
      (key, existing) -> {
        if (existing == null) {
          return ImmutableList.of(definedQProfile);
        }
        return Stream.concat(existing.stream(), Stream.of(definedQProfile)).collect(MoreCollectors.toList(existing.size() + 1));
      });
    return definedQProfile;
  }

  public DefinedQProfile create(Language language, String profileName, boolean isDefault, org.sonar.api.rules.ActiveRule... rules) {
    return new DefinedQProfile.Builder()
      .setLanguage(language.getKey())
      .setName(profileName)
      .setDeclaredDefault(isDefault)
        .addRules(Arrays.asList(rules))
      .build(DigestUtils.getMd5Digest());
  }
}
