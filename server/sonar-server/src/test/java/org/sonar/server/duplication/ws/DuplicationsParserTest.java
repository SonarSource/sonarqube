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

package org.sonar.server.duplication.ws;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DuplicationsParserTest {

  @Mock
  ComponentDao componentDao;

  @Mock
  DbSession session;

  ComponentDto currentFile;
  ComponentDto fileOnSameProject;
  ComponentDto fileOnDifferentProject;

  DuplicationsParser parser;

  ComponentDto project1;
  ComponentDto project2;

  @Before
  public void setUp() throws Exception {
    project1 = ComponentTesting.newProjectDto()
      .setId(1L)
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setKey("org.codehaus.sonar:sonar");

    project2 = ComponentTesting.newProjectDto().setId(2L);

    // Current file
    String key1 = "org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/utils/command/CommandExecutor.java";
    currentFile = ComponentTesting.newFileDto(project1).setId(10L).setKey(key1).setLongName("CommandExecutor");
    when(componentDao.getNullableByKey(session, key1)).thenReturn(currentFile);

    // File on same project
    String key2 = "org.codehaus.sonar:sonar-plugin-api:src/main/java/com/sonar/orchestrator/util/CommandExecutor.java";
    fileOnSameProject = ComponentTesting.newFileDto(project1).setId(11L).setKey(key2).setLongName("CommandExecutor");
    when(componentDao.getNullableByKey(session, key2)).thenReturn(fileOnSameProject);

    // File on different project
    String key3 = "com.sonarsource.orchestrator:sonar-orchestrator:src/main/java/com/sonar/orchestrator/util/CommandExecutor.java";
    fileOnDifferentProject = ComponentTesting.newFileDto(project2).setId(12L).setKey(key3).setLongName("CommandExecutor");
    when(componentDao.getNullableByKey(session, key3)).thenReturn(fileOnDifferentProject);

    parser = new DuplicationsParser(componentDao);
  }

  @Test
  public void empty_list_when_no_data() throws Exception {
    assertThat(parser.parse(currentFile, null, session)).isEmpty();
  }

  @Test
  public void duplication_on_same_file() throws Exception {
    List<DuplicationsParser.Block> blocks = parser.parse(currentFile, getData("duplication_on_same_file.xml"), session);
    assertThat(blocks).hasSize(1);

    List<DuplicationsParser.Duplication> duplications = blocks.get(0).duplications();
    assertThat(duplications).hasSize(2);

    // Smallest line comes first
    DuplicationsParser.Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.file()).isEqualTo(currentFile);
    assertThat(duplication1.from()).isEqualTo(20);
    assertThat(duplication1.size()).isEqualTo(5);

    DuplicationsParser.Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.file()).isEqualTo(currentFile);
    assertThat(duplication2.from()).isEqualTo(31);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void duplication_on_same_project() throws Exception {
    List<DuplicationsParser.Block> blocks = parser.parse(currentFile, getData("duplication_on_same_project.xml"), session);
    assertThat(blocks).hasSize(1);

    List<DuplicationsParser.Duplication> duplications = blocks.get(0).duplications();
    assertThat(duplications).hasSize(2);

    // Current file comes first
    DuplicationsParser.Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.file()).isEqualTo(currentFile);
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    DuplicationsParser.Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.file()).isEqualTo(fileOnSameProject);
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void duplications_on_different_project() throws Exception {
    List<DuplicationsParser.Block> blocks = parser.parse(currentFile, getData("duplications_on_different_project.xml"), session);
    assertThat(blocks).hasSize(1);

    List<DuplicationsParser.Duplication> duplications = blocks.get(0).duplications();
    assertThat(duplications).hasSize(3);

    // Current file's project comes first

    DuplicationsParser.Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.file()).isEqualTo(currentFile);
    assertThat(duplication1.from()).isEqualTo(148);
    assertThat(duplication1.size()).isEqualTo(24);

    DuplicationsParser.Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.file()).isEqualTo(fileOnSameProject);
    assertThat(duplication2.from()).isEqualTo(111);
    assertThat(duplication2.size()).isEqualTo(24);

    // Other project comes last

    DuplicationsParser.Duplication duplication3 = duplications.get(2);
    assertThat(duplication3.file()).isEqualTo(fileOnDifferentProject);
    assertThat(duplication3.from()).isEqualTo(137);
    assertThat(duplication3.size()).isEqualTo(24);
  }

  @Test
  public void duplications_on_many_blocks() throws Exception {
    List<DuplicationsParser.Block> blocks = parser.parse(currentFile, getData("duplications_on_many_blocks.xml"), session);
    assertThat(blocks).hasSize(2);

    // Block with smaller line should come first

    assertThat(blocks.get(0).duplications().get(0).from()).isEqualTo(38);
    assertThat(blocks.get(0).duplications().get(1).from()).isEqualTo(29);

    assertThat(blocks.get(1).duplications().get(0).from()).isEqualTo(94);
    assertThat(blocks.get(1).duplications().get(1).from()).isEqualTo(83);
  }

  @Test
  public void duplication_on_removed_file() throws Exception {
    List<DuplicationsParser.Block> blocks = parser.parse(currentFile, getData("duplication_on_removed_file.xml"), session);
    assertThat(blocks).hasSize(1);

    List<DuplicationsParser.Duplication> duplications = blocks.get(0).duplications();
    assertThat(duplications).hasSize(2);

    // Duplications on removed file
    DuplicationsParser.Duplication duplication1 = duplication(duplications, null);
    assertThat(duplication1.file()).isNull();
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    DuplicationsParser.Duplication duplication2 = duplication(duplications, fileOnSameProject.key());
    assertThat(duplication2.file()).isEqualTo(fileOnSameProject);
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void compare_duplications() throws Exception {
    ComponentDto currentFile = ComponentTesting.newFileDto(project1).setId(11L);
    ComponentDto fileOnSameProject = ComponentTesting.newFileDto(project1).setId(12L);
    ComponentDto fileOnDifferentProject = ComponentTesting.newFileDto(project2).setId(13L);

    DuplicationsParser.DuplicationComparator comparator = new DuplicationsParser.DuplicationComparator(currentFile.uuid(), currentFile.projectUuid());

    // On same file
    assertThat(comparator.compare(new DuplicationsParser.Duplication(currentFile, 2, 2), new DuplicationsParser.Duplication(currentFile, 5, 2))).isEqualTo(-1);
    // Different files on same project
    assertThat(comparator.compare(new DuplicationsParser.Duplication(currentFile, 2, 2), new DuplicationsParser.Duplication(fileOnSameProject, 5, 2))).isEqualTo(-1);
    assertThat(comparator.compare(new DuplicationsParser.Duplication(fileOnSameProject, 2, 2), new DuplicationsParser.Duplication(currentFile, 5, 2))).isEqualTo(1);
    // Different files on different projects
    assertThat(comparator.compare(new DuplicationsParser.Duplication(fileOnSameProject, 5, 2), new DuplicationsParser.Duplication(fileOnDifferentProject, 2, 2))).isEqualTo(-1);
    assertThat(comparator.compare(new DuplicationsParser.Duplication(fileOnDifferentProject, 5, 2), new DuplicationsParser.Duplication(fileOnSameProject, 2, 2))).isEqualTo(1);
    // Files on 2 different projects
    ComponentDto project3 = ComponentTesting.newProjectDto().setId(3L);
    assertThat(comparator.compare(new DuplicationsParser.Duplication(fileOnDifferentProject, 5, 2),
      new DuplicationsParser.Duplication(project3, 2, 2))).isEqualTo(1);

    // With null duplications
    assertThat(comparator.compare(null, new DuplicationsParser.Duplication(fileOnSameProject, 2, 2))).isEqualTo(-1);
    assertThat(comparator.compare(new DuplicationsParser.Duplication(fileOnSameProject, 2, 2), null)).isEqualTo(-1);
    assertThat(comparator.compare(null, null)).isEqualTo(-1);

    // On some removed file
    assertThat(comparator.compare(new DuplicationsParser.Duplication(currentFile, 2, 2), new DuplicationsParser.Duplication(null, 5, 2))).isEqualTo(-1);
    assertThat(comparator.compare(new DuplicationsParser.Duplication(null, 2, 2), new DuplicationsParser.Duplication(currentFile, 5, 2))).isEqualTo(-1);
  }

  private String getData(String file) throws IOException {
    return Files.toString(new File(Resources.getResource(this.getClass(), "DuplicationsParserTest/" + file).getFile()), Charsets.UTF_8);
  }

  private static DuplicationsParser.Duplication duplication(List<DuplicationsParser.Duplication> duplications, @Nullable final String componentKey){
    return Iterables.find(duplications, new Predicate<DuplicationsParser.Duplication>() {
      @Override
      public boolean apply(@Nullable DuplicationsParser.Duplication input) {
        return input != null && (componentKey == null ? input.file() == null : input.file() != null && componentKey.equals(input.file().key()));
      }
    });
  }

}
