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
package org.sonar.scanner.rule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QProfileVerifierTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private InputComponentStore store;
  private QualityProfiles profiles;

  @Before
  public void before() throws Exception {
    store = new InputComponentStore(mock(BranchConfiguration.class));
    profiles = mock(QualityProfiles.class);
    QProfile javaProfile = new QProfile("p1", "My Java profile", "java", null);
    when(profiles.findByLanguage("java")).thenReturn(javaProfile);
    QProfile cobolProfile = new QProfile("p2", "My Cobol profile", "cobol", null);
    when(profiles.findByLanguage("cobol")).thenReturn(cobolProfile);
  }

  @Test
  public void should_log_all_used_profiles() {
    store.put("foo", new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());
    store.put("foo", new TestInputFileBuilder("foo", "src/Baz.cbl").setLanguage("cobol").build());

    QProfileVerifier profileLogger = new QProfileVerifier(store, profiles);
    Logger logger = mock(Logger.class);
    profileLogger.execute(logger);

    verify(logger).info("Quality profile for {}: {}", "java", "My Java profile");
    verify(logger).info("Quality profile for {}: {}", "cobol", "My Cobol profile");
  }

  @Test
  public void should_not_fail_if_no_language_on_project() {
    QProfileVerifier profileLogger = new QProfileVerifier(store, profiles);

    profileLogger.execute();
  }

  @Test
  public void should_not_fail_if_default_profile_used_at_least_once() {
    store.put("foo", new TestInputFileBuilder("foo", "src/Bar.java").setLanguage("java").build());

    QProfileVerifier profileLogger = new QProfileVerifier(store, profiles);

    profileLogger.execute();
  }
}
