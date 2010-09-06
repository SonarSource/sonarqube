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
package org.sonar.api;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.batch.Sensor;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.rules.RulesRepository;

public class FakePlugin implements Plugin {
  private Language lang;
  private String key;
  private Class<? extends RulesRepository> rulesClass;
  private Class<? extends Sensor> sensor = null;

  public FakePlugin(String key, Language lang) {
    this.lang = lang;
    this.key = key;
  }

  public FakePlugin(String key, Language lang, Class<? extends RulesRepository> rulesClass) {
    this.lang = lang;
    this.key = key;
    this.rulesClass = rulesClass;
  }

  public FakePlugin(String key, Language lang, Class<? extends RulesRepository> rulesClass, Class<? extends Sensor> sensor) {
    this.lang = lang;
    this.key = key;
    this.rulesClass = rulesClass;
    this.sensor = sensor;
  }

  FakePlugin() {
    this("fake", new AbstractLanguage("fake", "Fake") {
      public String[] getFileSuffixes() {
        return new String[0];
      }
    });
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return null;
  }

  public String getDescription() {
    return null;
  }

  public Language getLanguage() {
    return lang;
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();
    if (sensor != null) {
      list.add(sensor);
    }
    if (rulesClass != null) {
      list.add(rulesClass);
    }
    return list;
  }


}
