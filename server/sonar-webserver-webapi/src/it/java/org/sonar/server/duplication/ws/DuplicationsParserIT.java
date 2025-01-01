/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class DuplicationsParserIT {

  @Rule
  public DbTester db = DbTester.create();

  private final DuplicationsParser parser = new DuplicationsParser(new ComponentFinder(db.getDbClient(), null));

  @Test
  public void empty_list_when_no_data() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    assertThat(parser.parse(db.getSession(), file, null, null, null)).isEmpty();
  }

  @Test
  public void duplication_on_same_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file, null, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
        "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file.getKey(), file.getKey()));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(2);

    // Smallest line comes first
    Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.componentDto()).isEqualTo(file);
    assertThat(duplication1.from()).isEqualTo(20);
    assertThat(duplication1.size()).isEqualTo(5);

    Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.componentDto()).isEqualTo(file);
    assertThat(duplication2.from()).isEqualTo(31);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void duplication_on_same_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file1, null, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
        "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file2.getKey(), file1.getKey()));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(2);

    // Current file comes first
    Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.componentDto()).isEqualTo(file1);
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.componentDto()).isEqualTo(file2);
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void duplications_on_different_project() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project1));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto fileOnProject2 = db.components().insertComponent(newFileDto(project2));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file1, null, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"148\" l=\"24\" r=\"%s\"/>\n" +
        "    <b s=\"137\" l=\"24\" r=\"%s\"/>\n" +
        "    <b s=\"111\" l=\"24\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file1.getKey(), fileOnProject2.getKey(), file2.getKey()));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(3);

    // Current file's project comes first

    Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.componentDto()).isEqualTo(file1);
    assertThat(duplication1.from()).isEqualTo(148);
    assertThat(duplication1.size()).isEqualTo(24);

    Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.componentDto()).isEqualTo(file2);
    assertThat(duplication2.from()).isEqualTo(111);
    assertThat(duplication2.size()).isEqualTo(24);

    // Other project comes last

    Duplication duplication3 = duplications.get(2);
    assertThat(duplication3.componentDto()).isEqualTo(fileOnProject2);
    assertThat(duplication3.from()).isEqualTo(137);
    assertThat(duplication3.size()).isEqualTo(24);
  }

  @Test
  public void duplications_on_many_blocks() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1)
      .setKey("org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/utils/command/CommandExecutor.java")
      .setLongName("CommandExecutor"));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2)
      .setKey("com.sonarsource.orchestrator:sonar-orchestrator:src/main/java/com/sonar/orchestrator/util/CommandExecutor.java")
      .setLongName("CommandExecutor"));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file1, null, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"94\" l=\"101\" r=\"%s\"/>\n" +
        "    <b s=\"83\" l=\"101\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "  <g>\n" +
        "    <b s=\"38\" l=\"40\" r=\"%s\"/>\n" +
        "    <b s=\"29\" l=\"39\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>\n", file2.getKey(), file1.getKey(), file2.getKey(), file1.getKey()));
    assertThat(blocks).hasSize(2);

    // Block with smaller line should come first

    assertThat(blocks.get(0).getDuplications().get(0).from()).isEqualTo(29);
    assertThat(blocks.get(0).getDuplications().get(1).from()).isEqualTo(38);

    assertThat(blocks.get(1).getDuplications().get(0).from()).isEqualTo(83);
    assertThat(blocks.get(1).getDuplications().get(1).from()).isEqualTo(94);
  }

  @Test
  public void duplication_on_not_existing_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file, null, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
        "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file.getKey(), "not_existing"));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(2);

    // Duplications on removed file
    Duplication duplication1 = duplication(duplications, null);
    assertThat(duplication1.componentDto()).isNull();
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    Duplication duplication2 = duplication(duplications, file.getKey());
    assertThat(duplication2.componentDto()).isEqualTo(file);
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void compare_duplications() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto currentFile = db.components().insertComponent(newFileDto(project1));
    ComponentDto fileOnSameProject = db.components().insertComponent(newFileDto(project1));
    ComponentDto fileOnDifferentProject = db.components().insertComponent(newFileDto(project2));

    DuplicationsParser.DuplicationComparator comparator = new DuplicationsParser.DuplicationComparator(currentFile.uuid(), currentFile.branchUuid());

    // On same file
    assertThat(comparator.compare(Duplication.newComponent(currentFile, 2, 2),
      Duplication.newComponent(currentFile, 5, 2))).isEqualTo(-1);
    // Different files on same project
    assertThat(comparator.compare(Duplication.newComponent(currentFile, 2, 2),
      Duplication.newComponent(fileOnSameProject, 5, 2))).isEqualTo(-1);
    assertThat(comparator.compare(Duplication.newComponent(fileOnSameProject, 2, 2),
      Duplication.newComponent(currentFile, 5, 2))).isOne();
    // Different files on different projects
    assertThat(comparator.compare(Duplication.newComponent(fileOnSameProject, 5, 2),
      Duplication.newComponent(fileOnDifferentProject, 2, 2))).isEqualTo(-1);
    assertThat(comparator.compare(Duplication.newComponent(fileOnDifferentProject, 5, 2),
      Duplication.newComponent(fileOnSameProject, 2, 2))).isOne();
    // Files on 2 different projects
    ComponentDto project3 = db.components().insertPrivateProject().getMainBranchComponent();
    assertThat(comparator.compare(Duplication.newComponent(fileOnDifferentProject, 5, 2),
      Duplication.newComponent(project3, 2, 2))).isOne();

    // With null duplications
    assertThat(comparator.compare(null, Duplication.newComponent(fileOnSameProject, 2, 2))).isEqualTo(-1);
    assertThat(comparator.compare(Duplication.newComponent(fileOnSameProject, 2, 2), null)).isEqualTo(-1);
    assertThat(comparator.compare(null, null)).isEqualTo(-1);

    // On some removed file
    assertThat(comparator.compare(Duplication.newComponent(currentFile, 2, 2),
      Duplication.newRemovedComponent("key1", 5, 2))).isEqualTo(-1);
    assertThat(comparator.compare(Duplication.newRemovedComponent("key2", 2, 2),
      Duplication.newComponent(currentFile, 5, 2))).isOne();
  }

  @Test
  public void duplication_on_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    ComponentDto file1 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    ComponentDto file2 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file1, branchName, null,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
        "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file2.getKey(), file1.getKey()));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(2);

    // Current file comes first
    Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.componentDto()).isEqualTo(file1);
    assertThat(duplication1.componentDto().getKey()).isEqualTo(file1.getKey());
    assertThat(duplication1.componentDto().branchUuid()).isEqualTo(branch.uuid());
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.componentDto()).isEqualTo(file2);
    assertThat(duplication2.componentDto().getKey()).isEqualTo(file2.getKey());
    assertThat(duplication1.componentDto().branchUuid()).isEqualTo(branch.uuid());
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  @Test
  public void duplication_on_pull_request() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    String pullRequestKey = RandomStringUtils.secure().nextAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey(pullRequestKey));
    ComponentDto file1 = db.components().insertComponent(newFileDto(pullRequest));
    ComponentDto file2 = db.components().insertComponent(newFileDto(pullRequest));
    List<DuplicationsParser.Block> blocks = parser.parse(db.getSession(), file1, null, pullRequestKey,
      format("<duplications>\n" +
        "  <g>\n" +
        "    <b s=\"20\" l=\"5\" r=\"%s\"/>\n" +
        "    <b s=\"31\" l=\"5\" r=\"%s\"/>\n" +
        "  </g>\n" +
        "</duplications>", file2.getKey(), file1.getKey()));
    assertThat(blocks).hasSize(1);

    List<Duplication> duplications = blocks.get(0).getDuplications();
    assertThat(duplications).hasSize(2);

    // Current file comes first
    Duplication duplication1 = duplications.get(0);
    assertThat(duplication1.componentDto()).isEqualTo(file1);
    assertThat(duplication1.componentDto().getKey()).isEqualTo(file1.getKey());
    assertThat(duplication1.componentDto().branchUuid()).isEqualTo(pullRequest.uuid());
    assertThat(duplication1.from()).isEqualTo(31);
    assertThat(duplication1.size()).isEqualTo(5);

    Duplication duplication2 = duplications.get(1);
    assertThat(duplication2.componentDto()).isEqualTo(file2);
    assertThat(duplication2.componentDto().getKey()).isEqualTo(file2.getKey());
    assertThat(duplication1.componentDto().branchUuid()).isEqualTo(pullRequest.uuid());
    assertThat(duplication2.from()).isEqualTo(20);
    assertThat(duplication2.size()).isEqualTo(5);
  }

  private static Duplication duplication(List<Duplication> duplications, @Nullable final String componentKey) {
    return Iterables.find(duplications, input -> input != null && (componentKey == null ? input.componentDto() == null
      : input.componentDto() != null && componentKey.equals(input.componentDto().getKey())));
  }

}
