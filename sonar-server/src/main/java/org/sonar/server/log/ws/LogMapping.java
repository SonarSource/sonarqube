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
package org.sonar.server.log.ws;

import org.sonar.api.resources.Languages;
import org.sonar.server.log.index.LogNormalizer;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.text.MacroInterpreter;

/**
 * Conversion between Log and WS JSON response
 */
public class LogMapping extends BaseMapping {


  public LogMapping(Languages languages, MacroInterpreter macroInterpreter) {
    super();
    addIndexStringField("key", LogNormalizer.LogFields.KEY.field());
    addIndexStringField("type", LogNormalizer.LogFields.TYPE.field());
    addIndexDatetimeField("createdAt", LogNormalizer.LogFields.DATE.field());
    addIndexStringField("userLogin", LogNormalizer.LogFields.AUTHOR.field());
    addIndexStringField("message", LogNormalizer.LogFields.MESSAGE.field());
    addIndexStringField("executionTime", LogNormalizer.LogFields.EXECUTION.field());
  }
}
