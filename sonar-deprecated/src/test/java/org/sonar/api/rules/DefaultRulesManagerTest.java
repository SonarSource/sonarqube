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

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.api.resources.Language;
import org.sonar.jpa.dao.DaoFacade;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultRulesManagerTest {

  @Test
  public void shouldReturnsZeroImportablePluginsWhenLanguageHasNoRulesPlugin() {
    DefaultRulesManager rulesManager = createRulesManagerForAlanguageWithNoPlugins();

    Language language = mock(Language.class);
    List<Plugin> result = rulesManager.getImportablePlugins(language);
    assertThat(result.size(), is(0));
  }

  @Test
  public void shouldReturnZeroPluginWhenLanguageHasNoRulesPlugin() {
    DefaultRulesManager rulesManager = createRulesManagerForAlanguageWithNoPlugins();
    Language language = mock(Language.class);
    List<Plugin> result = rulesManager.getPlugins(language);
    assertThat(result.size(), is(0));
  }

  @Test
  public void shouldReturnZeroRulesRepositoryWhenLanguageHasNoRulesRepository() {
    DefaultRulesManager rulesManager = createRulesManagerForAlanguageWithNoPlugins();
    Language language = mock(Language.class);
    List<RulesRepository<?>> result = rulesManager.getRulesRepositories(language);
    assertThat(result.size(), is(0));
  }

  private DefaultRulesManager createRulesManagerForAlanguageWithNoPlugins() {
    DaoFacade dao = mock(DaoFacade.class);
    RulesDao rulesDao = mock(RulesDao.class);
    when(rulesDao.getCategories()).thenReturn(Collections.<RulesCategory>emptyList());
    DefaultRulesManager rulesManager = new DefaultRulesManager(rulesDao, new Plugins(null));
    return rulesManager;
  }
}
