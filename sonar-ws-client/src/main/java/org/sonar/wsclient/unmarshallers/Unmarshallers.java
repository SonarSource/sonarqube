/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.wsclient.services.*;

import java.util.HashMap;
import java.util.Map;

public final class Unmarshallers {
  private Unmarshallers() {
  }

  private static Map<Class, Unmarshaller> unmarshallers;

  static {
    unmarshallers = new HashMap<Class, Unmarshaller>();
    unmarshallers.put(Metric.class, new MetricUnmarshaller());
    unmarshallers.put(Dependency.class, new DependencyUnmarshaller());
    unmarshallers.put(Resource.class, new ResourceUnmarshaller());
    unmarshallers.put(Property.class, new PropertyUnmarshaller());
    unmarshallers.put(Source.class, new SourceUnmarshaller());
    unmarshallers.put(Violation.class, new ViolationUnmarshaller());
    unmarshallers.put(Server.class, new ServerUnmarshaller());
    unmarshallers.put(ServerSetup.class, new ServerSetupUnmarshaller());
    unmarshallers.put(DependencyTree.class, new DependencyTreeUnmarshaller());
    unmarshallers.put(Event.class, new EventUnmarshaller());
    unmarshallers.put(Favourite.class, new FavouriteUnmarshaller());
    unmarshallers.put(Plugin.class, new PluginUnmarshaller());
    unmarshallers.put(Rule.class, new RuleUnmarshaller());
    unmarshallers.put(TimeMachine.class, new TimeMachineUnmarshaller());
    unmarshallers.put(Profile.class, new ProfileUnmarshaller());
    unmarshallers.put(Review.class, new ReviewUnmarshaller());
    unmarshallers.put(ManualMeasure.class, new ManualMeasureUnmarshaller());
    unmarshallers.put(Authentication.class, new AuthenticationUnmarshaller());
    unmarshallers.put(ResourceSearchResult.class, new ResourceSearchUnmarshaller());
  }

  public static <M extends Model> Unmarshaller<M> forModel(Class<M> modelClass) {
    return unmarshallers.get(modelClass);
  }
}
