/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.components;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PastViolationsLoaderTest extends AbstractDbUnitTestCase {

  private ResourcePersister resourcePersister;
  private PastViolationsLoader loader;

  @Before
  public void setUp() {
    setupData("shared");
    resourcePersister = mock(ResourcePersister.class);
    loader = new PastViolationsLoader(getSession(), resourcePersister);
  }

  @Test
  public void shouldGetPastResourceViolations() {
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", 1000);
    doReturn(snapshot).when(resourcePersister)
        .getSnapshot(any(Resource.class));
    doReturn(snapshot).when(resourcePersister)
        .getLastSnapshot(any(Snapshot.class), anyBoolean());

    List<RuleFailureModel> violations = loader.getPastViolations(new JavaFile("file"));

    assertThat(violations.size(), is(2));
  }

  @Test
  public void shouldReturnEmptyList() {
    List<RuleFailureModel> violations = loader.getPastViolations(null);

    assertThat(violations, notNullValue());
    assertThat(violations.size(), is(0));
  }

}
