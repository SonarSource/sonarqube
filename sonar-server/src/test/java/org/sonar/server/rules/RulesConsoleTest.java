/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rules;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RulesConsoleTest {
  @Test
  public void shouldIgnoreRepositoryExtensions() throws Exception {
    RuleRepository[] repositories = new RuleRepository[]{
        new FakeRepository("findbugs", "java"),
        new FakeRepository("findbugs", "java"), // for example fb-contrib
    };
    RulesConsole console = new RulesConsole(repositories);

    assertThat(console.getRepository("findbugs"), not(Matchers.nullValue()));
    assertThat(console.getRepositoriesByLanguage("java").size(), is(1));
  }

  private static class FakeRepository extends RuleRepository {

    private FakeRepository(String key, String language) {
      super(key, language);
    }

    @Override
    public List<Rule> createRules() {
      return Collections.emptyList();
    }
  }
}


