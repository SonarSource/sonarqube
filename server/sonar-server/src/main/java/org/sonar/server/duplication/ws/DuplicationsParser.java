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
package org.sonar.server.duplication.ws;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

@ServerSide
public class DuplicationsParser {
  private static final BlockComparator BLOCK_COMPARATOR = new BlockComparator();
  private final ComponentDao componentDao;

  public DuplicationsParser(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  public List<Block> parse(DbSession session, ComponentDto component, @Nullable String branch, @Nullable String pullRequest, @Nullable String duplicationsData) {
    Map<String, ComponentDto> componentsByKey = new LinkedHashMap<>();
    List<Block> blocks = new ArrayList<>();
    if (duplicationsData == null) {
      return blocks;
    }

    DuplicationComparator duplicationComparator = new DuplicationComparator(component.uuid(), component.projectUuid());

    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor root = inputFactory.rootElementCursor(new StringReader(duplicationsData));
      root.advance(); // <duplications>
      SMInputCursor cursor = root.childElementCursor("g");
      while (cursor.getNext() != null) {
        List<Duplication> duplications = new ArrayList<>();
        SMInputCursor bCursor = cursor.childElementCursor("b");
        while (bCursor.getNext() != null) {
          String from = bCursor.getAttrValue("s");
          String size = bCursor.getAttrValue("l");
          boolean disableLink = Boolean.parseBoolean(bCursor.getAttrValue("t"));
          String componentDbKey = bCursor.getAttrValue("r");
          if (from != null && size != null && componentDbKey != null) {
            if (disableLink) {
              // flag means that the target refers to an unchanged file in SLBs/PRs that doesn't exist in DB.
              // Display as text without a link or other details.
              duplications.add(Duplication.newTextComponent(componentDbKey, Integer.valueOf(from), Integer.valueOf(size)));
            } else {
              duplications.add(createDuplication(componentsByKey, branch, pullRequest, from, size, componentDbKey, session));
            }
          }
        }
        duplications.sort(duplicationComparator);
        blocks.add(new Block(duplications));
      }
      blocks.sort(BLOCK_COMPARATOR);
      return blocks;
    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
  }

  private Duplication createDuplication(Map<String, ComponentDto> componentsByKey, @Nullable String branch, @Nullable String pullRequest, String from,
    String size, String componentDbKey, DbSession session) {
    String componentKey = convertToKey(componentDbKey);

    ComponentDto component;
    if (componentsByKey.containsKey(componentKey)) {
      component = componentsByKey.get(componentKey);
    } else {
      component = loadComponent(session, componentKey, branch, pullRequest);
      componentsByKey.put(componentKey, component);
    }

    if (component != null) {
      return Duplication.newComponent(component, Integer.valueOf(from), Integer.valueOf(size));
    } else {
      //This can happen if the target was removed (cross-project duplications)
      return Duplication.newRemovedComponent(componentKey, Integer.valueOf(from), Integer.valueOf(size));
    }
  }

  @CheckForNull
  private ComponentDto loadComponent(DbSession session, String componentKey, @Nullable String branch, @Nullable String pullRequest) {
    if (branch != null) {
      return componentDao.selectByKeyAndBranch(session, componentKey, branch).orElse(null);
    } else if (pullRequest != null) {
      return componentDao.selectByKeyAndPullRequest(session, componentKey, pullRequest).orElse(null);
    } else {
      return componentDao.selectByKey(session, componentKey).orElse(null);
    }
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

  /**
   * Sorts the duplications with the following criteria:
   * - Duplications in the same file by starting line
   * - Duplications in the same project
   * - Cross project duplications
   */
  @VisibleForTesting
  static class DuplicationComparator implements Comparator<Duplication>, Serializable {
    private static final long serialVersionUID = 1;

    private final String fileUuid;
    private final String projectUuid;

    DuplicationComparator(String fileUuid, String projectUuid) {
      this.fileUuid = fileUuid;
      this.projectUuid = projectUuid;
    }

    @Override
    public int compare(@Nullable Duplication d1, @Nullable Duplication d2) {
      if (d1 == null || d2 == null) {
        return -1;
      }
      ComponentDto file1 = d1.componentDto();
      ComponentDto file2 = d2.componentDto();

      if (file1 != null && file1.equals(file2)) {
        // if duplication on same file => order by starting line
        return d1.from().compareTo(d2.from());
      }
      if (sameFile(file1) && !sameFile(file2)) {
        // the current resource must be displayed first
        return -1;
      }
      if (sameFile(file2) && !sameFile(file1)) {
        // the current resource must be displayed first
        return 1;
      }
      if (sameProject(file1) && !sameProject(file2)) {
        // if resource is in the same project, this it must be displayed first
        return -1;
      }
      if (sameProject(file2) && !sameProject(file1)) {
        // if resource is in the same project, this it must be displayed first
        return 1;
      }

      return d1.from().compareTo(d2.from());
    }

    private boolean sameFile(@Nullable ComponentDto otherDto) {
      return otherDto != null && StringUtils.equals(otherDto.uuid(), fileUuid);
    }

    private boolean sameProject(@Nullable ComponentDto otherDto) {
      return otherDto == null || StringUtils.equals(otherDto.projectUuid(), projectUuid);
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
