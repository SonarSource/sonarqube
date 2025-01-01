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

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.test.JsonAssert;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class ShowResponseBuilderIT {

  @Rule
  public DbTester db = DbTester.create();

  private ShowResponseBuilder underTest = new ShowResponseBuilder(db.getDbClient().componentDao());

  @Test
  public void write_duplications() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "path"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(project, dir));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project, dir));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file1, 57, 12),
      Duplication.newComponent(file2, 73, 12))));

    test(blocks, null, null,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12, \"_ref\": \"2\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file1.getKey() + "\",\n" +
        "      \"name\": \"" + file1.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    },\n" +
        "    \"2\": {\n" +
        "      \"key\": \"" + file2.getKey() + "\",\n" +
        "      \"name\": \"" + file2.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_duplications_without_sub_project() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file1, 57, 12),
      Duplication.newComponent(file2, 73, 12))));

    test(blocks, null, null,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12, \"_ref\": \"2\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file1.getKey() + "\",\n" +
        "      \"name\": \"" + file1.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    },\n" +
        "    \"2\": {\n" +
        "      \"key\": \"" + file2.getKey() + "\",\n" +
        "      \"name\": \"" + file2.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_duplications_with_a_removed_component() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file, 57, 12),
      // Duplication on a removed file
      Duplication.newRemovedComponent("key", 73, 12))));

    test(blocks, null, null,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file.getKey() + "\",\n" +
        "      \"name\": \"" + file.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_duplications_with_a_component_without_details() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file, 57, 12),
      // Duplication on a file without details
      Duplication.newTextComponent("project:path/to/file", 73, 12))));

    test(blocks, null, null,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file.getKey() + "\",\n" +
        "      \"name\": \"" + file.longName() + "\",\n" +
        "      \"project\": \"" + project.getKey() + "\",\n" +
        "      \"projectName\": \"" + project.longName() + "\",\n" +
        "    }\n" +
        "    \"2\": {\n" +
        "      \"key\": \"project:path/to/file\",\n" +
        "      \"name\": \"path/to/file\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_duplications_on_branch() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    ComponentDto file1 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    ComponentDto file2 = db.components().insertComponent(newFileDto(branch, project.uuid()));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file1, 57, 12),
      Duplication.newComponent(file2, 73, 12))));

    test(blocks, branchName, null,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12, \"_ref\": \"2\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file1.getKey() + "\",\n" +
        "      \"name\": \"" + file1.longName() + "\",\n" +
        "      \"project\": \"" + branch.getKey() + "\",\n" +
        "      \"projectName\": \"" + branch.longName() + "\",\n" +
        "      \"branch\": \"" + branchName + "\",\n" +
        "    },\n" +
        "    \"2\": {\n" +
        "      \"key\": \"" + file2.getKey() + "\",\n" +
        "      \"name\": \"" + file2.longName() + "\",\n" +
        "      \"project\": \"" + branch.getKey() + "\",\n" +
        "      \"projectName\": \"" + branch.longName() + "\",\n" +
        "      \"branch\": \"" + branchName + "\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_duplications_on_pull_request() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    String pullRequestKey = secure().nextAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey));
    ComponentDto file1 = db.components().insertComponent(newFileDto(pullRequest));
    ComponentDto file2 = db.components().insertComponent(newFileDto(pullRequest));
    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      Duplication.newComponent(file1, 57, 12),
      Duplication.newComponent(file2, 73, 12))));

    test(blocks, null, pullRequestKey,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12, \"_ref\": \"2\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"" + file1.getKey() + "\",\n" +
        "      \"name\": \"" + file1.longName() + "\",\n" +
        "      \"project\": \"" + pullRequest.getKey() + "\",\n" +
        "      \"projectName\": \"" + pullRequest.longName() + "\",\n" +
        "      \"pullRequest\": \"" + pullRequestKey + "\",\n" +
        "    },\n" +
        "    \"2\": {\n" +
        "      \"key\": \"" + file2.getKey() + "\",\n" +
        "      \"name\": \"" + file2.longName() + "\",\n" +
        "      \"project\": \"" + pullRequest.getKey() + "\",\n" +
        "      \"projectName\": \"" + pullRequest.longName() + "\",\n" +
        "      \"pullRequest\": \"" + pullRequestKey + "\",\n" +
        "    }\n" +
        "  }" +
        "}");
  }

  @Test
  public void write_nothing_when_no_data() {
    test(Collections.emptyList(), null, null, "{\"duplications\": [], \"files\": {}}");
  }

  private void test(List<DuplicationsParser.Block> blocks, @Nullable String branch, @Nullable String pullRequest, String expected) {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    ProtobufJsonFormat.write(underTest.build(db.getSession(), blocks, branch, pullRequest), jsonWriter);
    JsonAssert.assertJson(output.toString()).isSimilarTo(expected);
  }

}
