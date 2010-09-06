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
package org.sonar.api.checks.templates;

import org.junit.Test;
import org.sonar.api.checks.templates.CheckTemplateRepositories;
import org.sonar.api.checks.templates.CheckTemplateRepository;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckTemplateRepositoriesTest {

  @Test
  public void noRepositories() {
    CheckTemplateRepositories templateRepositories = new CheckTemplateRepositories();
    assertNull(templateRepositories.getRepository("foo"));
    assertThat(templateRepositories.getRepositories().size(), is(0));
  }

  @Test
  public void getRepositoryByKey() {
    CheckTemplateRepository repo1 = mock(CheckTemplateRepository.class);
    when(repo1.getKey()).thenReturn("one");

    CheckTemplateRepository repo2 = mock(CheckTemplateRepository.class);
    when(repo2.getKey()).thenReturn("two");

    CheckTemplateRepositories templateRepositories = new CheckTemplateRepositories(new CheckTemplateRepository[]{repo1, repo2});

    assertThat(templateRepositories.getRepositories().size(), is(2));
    assertEquals(repo1, templateRepositories.getRepository("one"));
    assertEquals(repo2, templateRepositories.getRepository("two"));
    assertNull(templateRepositories.getRepository("foo"));
  }
}
