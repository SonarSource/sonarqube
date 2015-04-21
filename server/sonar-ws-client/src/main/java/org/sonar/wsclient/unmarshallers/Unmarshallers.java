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
package org.sonar.wsclient.unmarshallers;

import org.sonar.wsclient.services.Authentication;
import org.sonar.wsclient.services.Event;
import org.sonar.wsclient.services.Favourite;
import org.sonar.wsclient.services.ManualMeasure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.Model;
import org.sonar.wsclient.services.Plugin;
import org.sonar.wsclient.services.Profile;
import org.sonar.wsclient.services.Property;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceSearchResult;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerSetup;
import org.sonar.wsclient.services.Source;
import org.sonar.wsclient.services.TimeMachine;

import java.util.HashMap;
import java.util.Map;

public final class Unmarshallers {
  private Unmarshallers() {
  }

  private static volatile Map<Class, Unmarshaller> unmarshallers;

  static {
    unmarshallers = new HashMap<>();
    unmarshallers.put(Metric.class, new MetricUnmarshaller());
    unmarshallers.put(Resource.class, new ResourceUnmarshaller());
    unmarshallers.put(Property.class, new PropertyUnmarshaller());
    unmarshallers.put(Source.class, new SourceUnmarshaller());
    unmarshallers.put(Server.class, new ServerUnmarshaller());
    unmarshallers.put(ServerSetup.class, new ServerSetupUnmarshaller());
    unmarshallers.put(Event.class, new EventUnmarshaller());
    unmarshallers.put(Favourite.class, new FavouriteUnmarshaller());
    unmarshallers.put(Plugin.class, new PluginUnmarshaller());
    unmarshallers.put(TimeMachine.class, new TimeMachineUnmarshaller());
    unmarshallers.put(Profile.class, new ProfileUnmarshaller());
    unmarshallers.put(ManualMeasure.class, new ManualMeasureUnmarshaller());
    unmarshallers.put(Authentication.class, new AuthenticationUnmarshaller());
    unmarshallers.put(ResourceSearchResult.class, new ResourceSearchUnmarshaller());
  }

  public static <M extends Model> Unmarshaller<M> forModel(Class<M> modelClass) {
    return unmarshallers.get(modelClass);
  }
}
