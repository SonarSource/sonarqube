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
package org.sonar.batch.indexer;

import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;

public class DefaultPersisterTest extends AbstractDbUnitTestCase {

  @Test
  public void createResource() {
    setupData("createResource");

    Bucket bucket = createBucket(new JavaPackage("org.foo"));

    new DefaultPersister(getSession()).persist(bucket);

    checkTables("createResource", "projects", "snapshots");
  }

  private Bucket createBucket(JavaPackage resource) {
    Bucket projectBucket = new Bucket(new Project("my:key").setId(5));
    projectBucket.setSnapshot(getSession().getSingleResult(Snapshot.class, "id", 30));

    Bucket bucket = new Bucket(resource);
    bucket.setProject(projectBucket);
    bucket.setParent(projectBucket);
    return bucket;
  }

  @Test
  public void updateExistingResource() {
    setupData("updateExistingResource");

    Bucket bucket = createBucket(new JavaPackage("org.foo"));

    new DefaultPersister(getSession()).persist(bucket);

    checkTables("updateExistingResource", "projects", "snapshots");
  }
}
