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
package org.sonar.server.duplication.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@ServerSide
public class DuplicationsParser {

  private final ComponentDao componentDao;

  public DuplicationsParser(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  public List<Block> parse(DbSession session, ComponentDto component, @Nullable String branch, @Nullable String duplicationsData) {
    Map<String, ComponentDto> componentsByKey = newHashMap();
    List<Block> blocks = newArrayList();
    if (duplicationsData != null) {
      try {
        SMInputFactory inputFactory = initStax();
        SMHierarchicCursor root = inputFactory.rootElementCursor(new StringReader(duplicationsData));
        root.advance(); // <duplications>
        SMInputCursor cursor = root.childElementCursor("g");
        while (cursor.getNext() != null) {
          List<Duplication> duplications = newArrayList();
          SMInputCursor bCursor = cursor.childElementCursor("b");
          while (bCursor.getNext() != null) {
            String from = bCursor.getAttrValue("s");
            String size = bCursor.getAttrValue("l");
            String componentKey = bCursor.getAttrValue("r");
            if (from != null && size != null && componentKey != null) {
              duplications.add(createDuplication(componentsByKey, branch, from, size, componentKey, session));
            }
          }
          Collections.sort(duplications, new DuplicationComparator(component.uuid(), component.projectUuid()));
          blocks.add(new Block(duplications));
        }
        Collections.sort(blocks, new BlockComparator());
      } catch (XMLStreamException e) {
        throw new IllegalStateException("XML is not valid", e);
      }
    }
    return blocks;
  }

  private Duplication createDuplication(Map<String, ComponentDto> componentsByKey, @Nullable String branch, String from, String size, String componentDbKey, DbSession session) {
    String componentKey = convertToKey(componentDbKey);
    ComponentDto component = componentsByKey.get(componentKey);
    if (component == null) {
      Optional<ComponentDto> componentDtoOptional = branch == null ? componentDao.selectByKey(session, componentKey)
        : Optional.fromNullable(componentDao.selectByKeyAndBranch(session, componentKey, branch).orElseGet(null));
      component = componentDtoOptional.isPresent() ? componentDtoOptional.get() : null;
      componentsByKey.put(componentKey, component);
    }
    return new Duplication(component, Integer.valueOf(from), Integer.valueOf(size));
  }

  private static String convertToKey(String dbKey) {
    return new ComponentDto().setDbKey(dbKey).getKey();
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  @VisibleForTesting
  static class DuplicationComparator implements Comparator<Duplication>, Serializable {
    private static final long serialVersionUID = 1;

    private final String uuid;
    private final String projectUuid;

    DuplicationComparator(String uuid, String projectUuid) {
      this.uuid = uuid;
      this.projectUuid = projectUuid;
    }

    @Override
    public int compare(@Nullable Duplication d1, @Nullable Duplication d2) {
      if (d1 == null || d2 == null) {
        return -1;
      }
      ComponentDto file1 = d1.file();
      ComponentDto file2 = d2.file();

      if (file1 == null || file2 == null) {
        return -1;
      }
      if (file1.equals(d2.file())) {
        // if duplication on same file => order by starting line
        return d1.from().compareTo(d2.from());
      }
      if (file1.uuid().equals(uuid)) {
        // the current resource must be displayed first
        return -1;
      }
      if (file2.uuid().equals(uuid)) {
        // the current resource must be displayed first
        return 1;
      }
      if (StringUtils.equals(file1.projectUuid(), projectUuid) && !StringUtils.equals(file2.projectUuid(), projectUuid)) {
        // if resource is in the same project, this it must be displayed first
        return -1;
      }
      if (StringUtils.equals(file2.projectUuid(), projectUuid) && !StringUtils.equals(file1.projectUuid(), projectUuid)) {
        // if resource is in the same project, this it must be displayed first
        return 1;
      }
      return d1.from().compareTo(d2.from());
    }
  }

  private static class BlockComparator implements Comparator<Block>, Serializable {
    private static final long serialVersionUID = 1;

    @Override
    public int compare(@Nullable Block b1, @Nullable Block b2) {
      if (b1 == null || b2 == null) {
        return -1;
      }
      List<Duplication> duplications1 = b1.getDuplications();
      List<Duplication> duplications2 = b2.getDuplications();
      if (duplications1.isEmpty() || duplications2.isEmpty()) {
        return -1;
      }
      return duplications1.get(0).from().compareTo(duplications2.get(0).from());
    }
  }

  public static class Duplication {
    private final ComponentDto file;
    private final Integer from;
    private final Integer size;

    Duplication(@Nullable ComponentDto file, Integer from, Integer size) {
      this.file = file;
      this.from = from;
      this.size = size;
    }

    /**
     * File can be null when duplication is linked on a removed file
     */
    @CheckForNull
    ComponentDto file() {
      return file;
    }

    Integer from() {
      return from;
    }

    Integer size() {
      return size;
    }
  }

  static class Block {
    private final List<Duplication> duplications;

    public Block(List<Duplication> duplications) {
      this.duplications = duplications;
    }

    public List<Duplication> getDuplications() {
      return duplications;
    }
  }

}
