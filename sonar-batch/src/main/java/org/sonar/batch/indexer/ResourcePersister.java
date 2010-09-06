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

import javax.persistence.NonUniqueResultException;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;

public abstract class ResourcePersister<RESOURCE extends Resource> {

  private DatabaseSession session;

  public ResourcePersister(DatabaseSession session) {
    this.session = session;
  }

  protected DatabaseSession getSession() {
    return session;
  }

  public final void persist(Bucket<RESOURCE> bucket) {
    String effectiveKey = generateEffectiveKey(bucket);
    ResourceModel model;
    try {
      model = session.getSingleResult(ResourceModel.class, "key", effectiveKey);
    } catch (NonUniqueResultException e) {
      throw new SonarException("The resource '" + effectiveKey + "' is duplicated in the Sonar DB.");
    }

    RESOURCE resource = bucket.getResource();
    if (model == null) {
      model = ResourceModel.build(resource);
      model.setKey(effectiveKey);

    } else {
      // update existing record
      model.setEnabled(true);
      if (StringUtils.isNotBlank(resource.getName())) {
        model.setName(resource.getName());
      }
      if (StringUtils.isNotBlank(resource.getLongName())) {
        model.setLongName(resource.getLongName());
      }
      if (StringUtils.isNotBlank(resource.getDescription())) {
        model.setDescription(resource.getDescription());
      }
      if ( !ResourceUtils.isLibrary(resource)) {
        model.setScope(resource.getScope());
        model.setQualifier(resource.getQualifier());
      }
      if (resource.getLanguage() != null) {
        model.setLanguageKey(resource.getLanguage().getKey());
      }
    }

    prepareResourceModel(model, bucket);
    model = session.save(model);
    resource.setId(model.getId());
    resource.setEffectiveKey(model.getKey());

    Snapshot snapshot = createSnapshot(bucket, model);
    if (snapshot.getId() == null) {
      session.save(snapshot);
    }
    bucket.setSnapshot(snapshot);
  }

  protected abstract void prepareResourceModel(ResourceModel resourceModel, Bucket<RESOURCE> bucket);

  protected abstract Snapshot createSnapshot(Bucket<RESOURCE> bucket, ResourceModel resourceModel);

  protected abstract String generateEffectiveKey(Bucket<RESOURCE> bucket);
}
