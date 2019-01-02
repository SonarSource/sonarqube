/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db;

import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.session.Configuration;

/**
 * Subclass of {@link XMLMapperBuilder} which fixes the fact that {@link XMLMapperBuilder} does not support loading
 * Mapper interfaces which belongs to the ClassLoader of a plugin.
 */
public class SQXMLMapperBuilder extends XMLMapperBuilder {
  private final Class<?> mapperType;

  public SQXMLMapperBuilder(Class<?> mapperType, InputStream inputStream, Configuration configuration, Map<String, XNode> sqlFragments) {
    super(inputStream, configuration, mapperType.getName(), sqlFragments);
    this.mapperType = mapperType;
  }

  @Override
  public void parse() {
    if (!configuration.isResourceLoaded(mapperType.getName())) {
      super.parse();
      retryBindMapperForNamespace();
    }

    super.parse();
  }

  private void retryBindMapperForNamespace() {
    if (!configuration.hasMapper(mapperType)) {
      // Spring may not know the real resource name so we set a flag
      // to prevent loading again this resource from the mapper interface
      // look at MapperAnnotationBuilder#loadXmlResource
      configuration.addLoadedResource("namespace:" + mapperType.getName());
      configuration.addMapper(mapperType);
    }
  }
}
