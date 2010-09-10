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
package org.sonar.api.profiles;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ValidationMessages;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @since 2.3
 */
public abstract class XMLProfileDefinition extends ProfileDefinition {

  private String name;
  private String language;
  private ClassLoader classloader;
  private String xmlClassPath;

  protected XMLProfileDefinition(String name, String language, ClassLoader classloader, String xmlClassPath) {
    this.name = name;
    this.language = language;
    this.classloader = classloader;
    this.xmlClassPath = xmlClassPath;
  }

  @Override
  public final ProfilePrototype createPrototype(ValidationMessages validation) {
    Reader reader = new InputStreamReader(classloader.getResourceAsStream(xmlClassPath), Charset.forName(CharEncoding.UTF_8));
    try {
      ProfilePrototype profile = XMLProfileImporter.create().importProfile(reader, validation);
      profile.setName(name);
      profile.setLanguage(language);
      return profile;

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
