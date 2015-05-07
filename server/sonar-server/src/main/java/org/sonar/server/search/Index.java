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
package org.sonar.server.search;

import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.Dto;

import javax.annotation.CheckForNull;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

@ServerSide
public interface Index<DOMAIN, DTO extends Dto<KEY>, KEY extends Serializable> extends Startable {

  @CheckForNull
  DOMAIN getNullableByKey(KEY key);

  String getIndexType();

  String getIndexName();

  @CheckForNull
  Date getLastSynchronization();

  @CheckForNull
  Date getLastSynchronization(Map<String, String> params);

  IndexStat getIndexStat();

  Iterator<DOMAIN> scroll(String scrollId);

  BaseNormalizer<DTO, KEY> getNormalizer();
}
