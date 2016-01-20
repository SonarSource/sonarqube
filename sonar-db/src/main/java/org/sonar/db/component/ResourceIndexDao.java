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
package org.sonar.db.component;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.db.AbstractDao;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;

public class ResourceIndexDao extends AbstractDao {

  private static final String SELECT_RESOURCES = "org.sonar.db.component.ResourceIndexMapper.selectResources";
  public static final int MINIMUM_KEY_SIZE = 3;
  public static final int SINGLE_INDEX_SIZE = 2;

  // The scopes and qualifiers that are not in the following constants are not indexed at all.
  // Directories and packages are explicitly excluded.
  private static final String[] RENAMABLE_QUALIFIERS = {Qualifiers.PROJECT, Qualifiers.MODULE, Qualifiers.VIEW, Qualifiers.SUBVIEW};
  private static final String[] RENAMABLE_SCOPES = {Scopes.PROJECT};
  private static final String[] NOT_RENAMABLE_QUALIFIERS = {Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE, Qualifiers.CLASS};
  private static final String[] NOT_RENAMABLE_SCOPES = {Scopes.FILE};

  public ResourceIndexDao(MyBatis myBatis, System2 system2) {
    super(myBatis, system2);
  }

  public List<Long> selectProjectIdsFromQueryAndViewOrSubViewUuid(DbSession session, String query, String viewOrSubViewUuid) {
    return session.getMapper(ResourceIndexMapper.class).selectProjectIdsFromQueryAndViewOrSubViewUuid(query + "%", "%." + viewOrSubViewUuid + ".%");
  }

  /**
   * This method is reentrant. It can be executed even if the project is already indexed.
   */
  public ResourceIndexDao indexProject(final long rootProjectId) {
    DbSession session = myBatis().openSession(true);
    try {
      indexProject(rootProjectId, session);
      session.commit();
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void indexProject(final long rootProjectId, DbSession session) {
    ResourceIndexMapper mapper = session.getMapper(ResourceIndexMapper.class);
    doIndexProject(rootProjectId, session, mapper);
  }

  /**
   * This method is reentrant. It can be executed even if some projects are already indexed.
   */
  public ResourceIndexDao indexProjects() {
    final DbSession session = myBatis().openSession(true);
    try {
      final ResourceIndexMapper mapper = session.getMapper(ResourceIndexMapper.class);
      session.select(ResourceIndexMapper.class.getName() + ".selectRootProjectIds", /* workaround to get booleans */ResourceIndexQuery.create(), new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          Integer rootProjectId = (Integer) context.getResultObject();
          doIndexProject(rootProjectId, session, mapper);
          session.commit();
        }
      });
      return this;

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void doIndexProject(long rootProjectId, SqlSession session, final ResourceIndexMapper mapper) {
    // non indexed resources
    ResourceIndexQuery query = ResourceIndexQuery.create()
      .setNonIndexedOnly(true)
      .setQualifiers(NOT_RENAMABLE_QUALIFIERS)
      .setScopes(NOT_RENAMABLE_SCOPES)
      .setRootProjectId(rootProjectId);

    session.select(SELECT_RESOURCES, query, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        ResourceDto resource = (ResourceDto) context.getResultObject();
        doIndex(resource, mapper);
      }
    });

    // some resources can be renamed, so index must be regenerated
    // -> delete existing rows and create them again
    query = ResourceIndexQuery.create()
      .setNonIndexedOnly(false)
      .setQualifiers(RENAMABLE_QUALIFIERS)
      .setScopes(RENAMABLE_SCOPES)
      .setRootProjectId(rootProjectId);

    session.select(SELECT_RESOURCES, query, new ResultHandler() {
      @Override
      public void handleResult(ResultContext context) {
        ResourceDto resource = (ResourceDto) context.getResultObject();

        mapper.deleteByResourceId(resource.getId());
        doIndex(resource, mapper);
      }
    });
  }

  void doIndex(ResourceDto resource, ResourceIndexMapper mapper) {
    String key = nameToKey(resource.getName());
    if (key.length() >= MINIMUM_KEY_SIZE || key.length() == SINGLE_INDEX_SIZE) {
      insertIndexEntries(key, resource.getId(), resource.getQualifier(), resource.getRootId(), resource.getName().length(), mapper);
    }
  }

  public boolean indexResource(long id) {
    DbSession session = myBatis().openSession(false);
    try {
      return indexResource(session, id);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public boolean indexResource(DbSession session, long id) {
    boolean indexed = false;
    ResourceIndexMapper mapper = session.getMapper(ResourceIndexMapper.class);
    ResourceDto resource = mapper.selectResourceToIndex(id);
    if (resource != null) {
      Long rootId = resource.getRootId();
      if (rootId == null) {
        rootId = resource.getId();
      }
      indexed = indexResource(resource.getId(), resource.getName(), resource.getQualifier(), rootId, session, mapper);
    }
    return indexed;
  }

  public boolean indexResource(int id, String name, String qualifier, int rootId) {
    boolean indexed = false;
    SqlSession session = myBatis().openSession(false);
    ResourceIndexMapper mapper = session.getMapper(ResourceIndexMapper.class);
    try {
      indexed = indexResource(id, name, qualifier, rootId, session, mapper);
    } finally {
      MyBatis.closeQuietly(session);
    }
    return indexed;
  }

  private static boolean indexResource(long id, String name, String qualifier, long rootId, SqlSession session, ResourceIndexMapper mapper) {
    boolean indexed = false;
    String key = nameToKey(name);
    if (key.length() >= MINIMUM_KEY_SIZE || key.length() == SINGLE_INDEX_SIZE) {
      indexed = true;
      boolean toBeIndexed = sanitizeIndex(id, key, mapper);
      if (toBeIndexed) {
        insertIndexEntries(key, id, qualifier, rootId, name.length(), mapper);
        session.commit();
      }
    }
    return indexed;
  }

  private static void insertIndexEntries(String key, long resourceId, String qualifier, long rootId, int nameLength, ResourceIndexMapper mapper) {
    ResourceIndexDto dto = new ResourceIndexDto()
      .setResourceId(resourceId)
      .setQualifier(qualifier)
      .setRootProjectId(rootId)
      .setNameSize(nameLength);

    int maxPosition = key.length() == SINGLE_INDEX_SIZE ? 0 : key.length() - MINIMUM_KEY_SIZE;
    for (int position = 0; position <= maxPosition; position++) {
      dto.setPosition(position);
      dto.setKey(StringUtils.substring(key, position));
      mapper.insert(dto);
    }
  }

  /**
   * Return true if the resource must be indexed, false if the resource is already indexed.
   * If the resource is indexed with a different key, then this index is dropped and the
   * resource must be indexed again.
   */
  private static boolean sanitizeIndex(long resourceId, String key, ResourceIndexMapper mapper) {
    ResourceIndexDto masterIndex = mapper.selectMasterIndexByResourceId(resourceId);
    if (masterIndex != null && !StringUtils.equals(key, masterIndex.getKey())) {
      // resource has been renamed -> drop existing indexes
      mapper.deleteByResourceId(resourceId);
      masterIndex = null;
    }
    return masterIndex == null;
  }

  static String nameToKey(String input) {
    return StringUtils.lowerCase(StringUtils.trimToEmpty(input));
  }
}
