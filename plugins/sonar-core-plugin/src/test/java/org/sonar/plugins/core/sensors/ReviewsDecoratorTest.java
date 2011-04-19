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
package org.sonar.plugins.core.sensors;

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReviewsDecoratorTest extends AbstractDbUnitTestCase {

  @Test
  @Ignore("DBUnit needs Hibernate mapping...")
  public void shouldSaveConfigurationInSnapshotsTable() throws ParseException {
    setupData("fixture");
    
    File resource = new File("Foo");
    Snapshot snapshot = new Snapshot();
    snapshot.setId(666);
    snapshot.setResourceId(111);
    ResourcePersister persister = mock(ResourcePersister.class);
    when(persister.getSnapshot(resource)).thenReturn(snapshot);

    ReviewsDecorator reviewsDecorator = new ReviewsDecorator(persister, getSession());
    reviewsDecorator.decorate(resource, null);

    //checkTables("shouldSaveConfigurationInSnapshotsTable", "snapshots");
  }
}
