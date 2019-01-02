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
import org.sonar.api.utils.ValidationMessages;

import java.io.Reader;

/**
 * Fake importer just for test, it will NOT take into account the given file but will display some info and warning messages
 */
public class XooFakeImporterWithMessages extends ProfileImporter {
  public XooFakeImporterWithMessages() {
    super("XooFakeImporterWithMessages", "Xoo Profile Importer With Messages");
  }

  @Override
  public String[] getSupportedLanguages() {
    return new String[] {};
  }

  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    messages.addWarningText("a warning");
    messages.addInfoText("an info");
    return RulesProfile.create();
  }
}
