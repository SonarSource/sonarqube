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
package org.sonar.server.es.newindex;

import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.FakeDoc;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexDefinition;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexMainType;

import static org.sonar.server.es.newindex.SettingsConfiguration.newBuilder;

public class FakeIndexDefinition implements IndexDefinition {

  public static final String INDEX = "fakes";
  public static final String TYPE = "fake";
  public static final Index DESCRIPTOR = Index.simple(INDEX);
  public static final IndexMainType TYPE_FAKE = IndexType.main(DESCRIPTOR, TYPE);
  public static final String INT_FIELD = "intField";

  private int replicas = 0;

  public FakeIndexDefinition setReplicas(int replicas) {
    this.replicas = replicas;
    return this;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(DESCRIPTOR, newBuilder(new MapSettings().asConfig()).build());
    index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicas);
    index.getSettings().put("index.refresh_interval", "-1");
    index.createTypeMapping(TYPE_FAKE)
      .createIntegerField(INT_FIELD);
  }

  public static FakeDoc newDoc(int value) {
    return new FakeDoc().setInt(value);
  }
}
