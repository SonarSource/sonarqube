/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.startup;

import org.junit.Test;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class EnableProfilesTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldDisableProfilesWithMissingLanguages() {
    setupData("shouldDisableProfilesWithMissingLanguages");

    Language[] languages = new Language[]{Java.INSTANCE, new Php()};
    EnableProfiles task = new EnableProfiles(languages, getSessionFactory(), null);
    task.start();

    checkTables("shouldDisableProfilesWithMissingLanguages", "rules_profiles");
  }

  @Test
  public void shouldEnableProfilesWithKnownLanguages() {
    setupData("shouldEnableProfilesWithKnownLanguages");

    Language[] languages = new Language[]{Java.INSTANCE, new Php()};
    EnableProfiles task = new EnableProfiles(languages, getSessionFactory(), null);
    task.start();

    checkTables("shouldEnableProfilesWithKnownLanguages", "rules_profiles");
  }

  private static class Php extends AbstractLanguage {

    public Php() {
      super("php");
    }

    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}

