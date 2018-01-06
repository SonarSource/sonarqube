/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.xoo.lang;

import net.sourceforge.pmd.cpd.Tokenizer;
import org.sonar.api.batch.AbstractCpdMapping;
import org.sonar.api.resources.Language;
import org.sonar.xoo.Xoo;

public class XooCpdMapping extends AbstractCpdMapping {

  private Xoo xoo;
  private XooTokenizer xooTokenizer;

  public XooCpdMapping(Xoo xoo, XooTokenizer xooTokenizer) {
    this.xoo = xoo;
    this.xooTokenizer = xooTokenizer;
  }

  @Override
  public Tokenizer getTokenizer() {
    return xooTokenizer;
  }

  @Override
  public Language getLanguage() {
    return xoo;
  }
}
