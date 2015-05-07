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
package org.sonar.api.batch;

import net.sourceforge.pmd.cpd.Tokenizer;
import org.sonar.api.BatchSide;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Resource;

import java.io.File;
import java.util.List;

/**
 * Implement this extension to get Copy/Paste detection for your language.
 * @since 1.10
 */
@BatchSide
@ExtensionPoint
public interface CpdMapping {

  Tokenizer getTokenizer();

  Language getLanguage();

  /**
   * @deprecated since 4.2 not used anymore
   */
  @Deprecated
  Resource createResource(File file, List<File> sourceDirs);

}
