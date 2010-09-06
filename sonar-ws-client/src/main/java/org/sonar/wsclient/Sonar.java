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
package org.sonar.wsclient;

import org.sonar.wsclient.connectors.Connector;
import org.sonar.wsclient.connectors.ConnectorFactory;
import org.sonar.wsclient.services.*;
import org.sonar.wsclient.unmarshallers.Unmarshaller;
import org.sonar.wsclient.unmarshallers.Unmarshallers;

import java.util.Collections;
import java.util.List;

public class Sonar {

  static {
    WSUtils.setInstance(new JdkUtils());
  }

  private Connector connector;

  public Sonar(Connector connector) {
    this.connector = connector;
  }

  public Connector getConnector() {
    return connector;
  }

  public <MODEL extends Model> MODEL find(Query<MODEL> query) {
    String json = connector.execute(query);
    MODEL result = null;
    if (json != null) {
      Unmarshaller<MODEL> unmarshaller = Unmarshallers.forModel(query.getModelClass());
      result = unmarshaller.toModel(json);
    }
    return result;
  }

  public <MODEL extends Model> List<MODEL> findAll(Query<MODEL> query) {
    String json = connector.execute(query);
    List<MODEL> result;
    if (json == null) {
      result = Collections.emptyList();
    } else {
      Unmarshaller<MODEL> unmarshaller = Unmarshallers.forModel(query.getModelClass());
      result = unmarshaller.toModels(json);
    }
    return result;
  }

  public <MODEL extends Model> MODEL create(CreateQuery<MODEL> query) {
    String json = connector.execute(query);
    MODEL result = null;
    if (json != null) {
      Unmarshaller<MODEL> unmarshaller = Unmarshallers.forModel(query.getModelClass());
      result = unmarshaller.toModel(json);
    }
    return result;
  }

  public void delete(DeleteQuery query) {
    connector.execute(query);
  }

  public static Sonar create(String host) {
    return new Sonar(ConnectorFactory.create(new Host(host)));
  }

  public static Sonar create(String host, String username, String password) {
    return new Sonar(ConnectorFactory.create(new Host(host, username, password)));
  }
}
