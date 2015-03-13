/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.es;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import java.util.Map;

public class FakeIndexDefinition implements IndexDefinition {

  public static final String INDEX = "fakes";
  public static final String TYPE = "fake";
  public static final String INT_FIELD = "intField";

  private int replicas = 0;

  public FakeIndexDefinition setReplicas(int replicas) {
    this.replicas = replicas;
    return this;
  }

  @Override
  public void define(IndexDefinitionContext context) {
    NewIndex index = context.create(INDEX);
    index.getSettings().put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, replicas);
    index.getSettings().put("index.refresh_interval", "-1");
    NewIndex.NewIndexType type = index.createType(TYPE);
    type.createIntegerField(INT_FIELD);
  }

  public static Map<String,Object> newDoc(int value) {
    return ImmutableMap.<String,Object>of(INT_FIELD, value);
  }
}
