/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.issue;

import java.util.List;
import javax.annotation.CheckForNull;

/**
 * @since 3.7
 */
public interface IssueFilterMapper {

  @CheckForNull
  IssueFilterDto selectById(long id);

  List<IssueFilterDto> selectByUser(String user);

  List<IssueFilterDto> selectFavoriteFiltersByUser(String user);

  List<IssueFilterDto> selectSharedFilters();

  IssueFilterDto selectProvidedFilterByName(String name);

  void insert(IssueFilterDto filter);

  void update(IssueFilterDto filter);

  void delete(long id);

}
