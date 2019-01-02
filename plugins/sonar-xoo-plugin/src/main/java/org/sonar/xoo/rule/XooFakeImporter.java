/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.xoo.rule;

import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.xoo.Xoo;

import java.io.Reader;

/**
 * Fake importer just for test, it will NOT take into account the given file but will create some hard-coded rules
 */
public class XooFakeImporter extends ProfileImporter {
  public XooFakeImporter() {
    super("XooProfileImporter", "Xoo Profile Importer");
  }

  @Override
  public String[] getSupportedLanguages() {
    return new String[] {Xoo.KEY};
  }

  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    RulesProfile rulesProfile = RulesProfile.create();
    rulesProfile.activateRule(Rule.create(XooRulesDefinition.XOO_REPOSITORY, "x1"), RulePriority.CRITICAL);
    return rulesProfile;
  }
}
