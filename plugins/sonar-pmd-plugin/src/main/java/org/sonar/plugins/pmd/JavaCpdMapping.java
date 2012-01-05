/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.pmd;

import java.io.File;
import java.util.List;
import java.util.Properties;

import net.sourceforge.pmd.cpd.JavaTokenizer;
import net.sourceforge.pmd.cpd.Tokenizer;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public class JavaCpdMapping implements CpdMapping {

  private String ignore_literals;
  private String ignore_identifiers;

  public JavaCpdMapping(Project project) {
    ignore_literals = project.getConfiguration().getString(CoreProperties.CPD_IGNORE_LITERALS_PROPERTY,
        CoreProperties.CPD_IGNORE_LITERALS_DEFAULT_VALUE);
    ignore_identifiers = project.getConfiguration().getString(CoreProperties.CPD_IGNORE_IDENTIFIERS_PROPERTY,
        CoreProperties.CPD_IGNORE_IDENTIFIERS_DEFAULT_VALUE);
  }

  public Tokenizer getTokenizer() {
    Properties props = new Properties();
    props.setProperty(JavaTokenizer.IGNORE_LITERALS, ignore_literals);
    props.setProperty(JavaTokenizer.IGNORE_IDENTIFIERS, ignore_identifiers);
    JavaTokenizer tokenizer = new JavaTokenizer();
    tokenizer.setProperties(props);
    return tokenizer;
  }

  public Resource createResource(File file, List<File> sourceDirs) {
    return JavaFile.fromIOFile(file, sourceDirs, false);
  }

  public Language getLanguage() {
    return Java.INSTANCE;
  }
}
