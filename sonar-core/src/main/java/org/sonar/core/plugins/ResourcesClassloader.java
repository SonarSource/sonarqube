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
package org.sonar.core.plugins;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

/**
 * This class loader is used to load resources from a list of URLs - see SONAR-1861.
 */
public class ResourcesClassloader extends URLClassLoader {
  private Collection<URL> urls;

  public ResourcesClassloader(Collection<URL> urls, ClassLoader parent) {
    super(new URL[] {}, parent);
    this.urls = Lists.newArrayList(urls);
  }

  @Override
  public URL findResource(String name) {
    for (URL url : urls) {
      if (StringUtils.endsWith(url.getPath(), name)) {
        return url;
      }
    }
    return null;
  }
}
