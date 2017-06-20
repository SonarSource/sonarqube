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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;

import static com.google.common.base.Preconditions.checkState;

public class BuiltInQProfileRepositoryRule extends ExternalResource implements BuiltInQProfileRepository {
  private boolean initializeCalled = false;
  private List<BuiltInQProfile> profiles = new ArrayList<>();

  @Override
  protected void before() throws Throwable {
    this.initializeCalled = false;
    this.profiles.clear();
  }

  @Override
  public void initialize() {
    checkState(!initializeCalled, "initialize must be called only once");
    this.initializeCalled = true;
  }

  @Override
  public List<BuiltInQProfile> get() {
    checkState(initializeCalled, "initialize must be called first");

    return ImmutableList.copyOf(profiles);
  }

  public boolean isInitialized() {
    return initializeCalled;
  }

  public BuiltInQProfile add(Language language, String profileName) {
    return add(language, profileName, false);
  }

  public BuiltInQProfile add(Language language, String profileName, boolean isDefault, org.sonar.api.rules.ActiveRule... rules) {
    BuiltInQProfile builtIn = create(language, profileName, isDefault, rules);
    profiles.add(builtIn);
    return builtIn;
  }

  public BuiltInQProfile create(Language language, String profileName, boolean isDefault, org.sonar.api.rules.ActiveRule... rules) {
    return new BuiltInQProfile.Builder()
      .setLanguage(language.getKey())
      .setName(profileName)
      .setDeclaredDefault(isDefault)
      .addRules(Arrays.asList(rules))
      .build();
  }

  public BuiltInQProfile create(RulesProfile api) {
    return new BuiltInQProfile.Builder()
      .setLanguage(api.getLanguage())
      .setName(api.getName())
      .setDeclaredDefault(api.getDefaultProfile())
      .addRules(new ArrayList<>(api.getActiveRules()))
      .build();
  }
}
