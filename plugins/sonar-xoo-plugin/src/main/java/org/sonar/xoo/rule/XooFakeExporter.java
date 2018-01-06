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
package org.sonar.xoo.rule;

import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.xoo.Xoo;

import java.io.IOException;
import java.io.Writer;

/**
 * Fake exporter just for test
 */
public class XooFakeExporter extends ProfileExporter {
  public XooFakeExporter() {
    super("XooFakeExporter", "Xoo Fake Exporter");
  }

  @Override
  public String[] getSupportedLanguages() {
    return new String[]{Xoo.KEY};
  }

  @Override
  public String getMimeType() {
    return "plain/custom";
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    try {
      writer.write("xoo -> " + profile.getName() + " -> " + profile.getActiveRules().size());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
