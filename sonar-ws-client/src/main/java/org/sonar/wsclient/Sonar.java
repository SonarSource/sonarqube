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
package org.sonar.wsclient;

import org.sonar.wsclient.connectors.Connector;
import org.sonar.wsclient.connectors.ConnectorFactory;
import org.sonar.wsclient.services.*;
import org.sonar.wsclient.unmarshallers.UnmarshalException;
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

  public <M extends Model> M find(Query<M> query) {
    String json = connector.execute(query);
    M result = null;
    if (json != null) {
      try {
        Unmarshaller<M> unmarshaller = Unmarshallers.forModel(query.getModelClass());
        result = unmarshaller.toModel(json);
      } catch (Exception e) {
        throw new UnmarshalException(query, json, e);
      }
    }
    return result;
  }

  public <M extends Model> List<M> findAll(Query<M> query) {
    String json = connector.execute(query);
    List<M> result;
    if (json == null) {
      result = Collections.emptyList();
    } else {
      try {
        Unmarshaller<M> unmarshaller = Unmarshallers.forModel(query.getModelClass());
        result = unmarshaller.toModels(json);
      } catch (Exception e) {
        throw new UnmarshalException(query, json, e);
      }
    }
    return result;
  }

  public <M extends Model> M create(CreateQuery<M> query) {
    String json = connector.execute(query);
    M result = null;
    if (json != null) {
      try {
        Unmarshaller<M> unmarshaller = Unmarshallers.forModel(query.getModelClass());
        result = unmarshaller.toModel(json);
      } catch (Exception e) {
        throw new UnmarshalException(query, json, e);
      }
    }
    return result;
  }

  public void update(UpdateQuery<?> query) {
    connector.execute(query);
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
