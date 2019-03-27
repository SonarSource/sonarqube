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
package org.sonar.ce.task.projectanalysis.notification;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.DumbRule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.issue.notification.MyNewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification;
import org.sonar.server.issue.notification.NewIssuesNotification.DetailsSupplier;
import org.sonar.server.issue.notification.NewIssuesNotification.RuleDefinition;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class NewIssuesNotificationFactoryTest {
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Durations durations = new Durations();
  private NewIssuesNotificationFactory underTest = new NewIssuesNotificationFactory(treeRootHolder, ruleRepository, durations);

  @Test
  public void newMyNewIssuesNotification_throws_NPE_if_assigneesByUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("assigneesByUuid can't be null");

    underTest.newMyNewIssuesNotification(null);
  }

  @Test
  public void newNewIssuesNotification_throws_NPE_if_assigneesByUuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("assigneesByUuid can't be null");

    underTest.newNewIssuesNotification(null);
  }

  @Test
  public void newMyNewIssuesNotification_returns_MyNewIssuesNotification_object_with_the_constructor_Durations() {
    MyNewIssuesNotification notification = underTest.newMyNewIssuesNotification(emptyMap());

    assertThat(readDurationsField(notification)).isSameAs(durations);
  }

  @Test
  public void newNewIssuesNotification_returns_NewIssuesNotification_object_with_the_constructor_Durations() {
    NewIssuesNotification notification = underTest.newNewIssuesNotification(emptyMap());

    assertThat(readDurationsField(notification)).isSameAs(durations);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_fails_with_NPE_if_uuid_is_null() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getUserNameByUuid(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_always_returns_empty_if_map_argument_is_empty() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_name_of_user_from_map_argument() {
    Set<UserDto> users = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> UserTesting.newUserDto().setLogin("user" + i))
      .collect(Collectors.toSet());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(
      users.stream().collect(uniqueIndex(UserDto::getUuid)));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
    users
      .forEach(user -> assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).contains(user.getName()));
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_empty_if_user_has_null_name() {
    UserDto user = UserTesting.newUserDto().setLogin("user_noname").setName(null);

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(ImmutableMap.of(user.getUuid(), user));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_fails_with_NPE_if_uuid_is_null() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getUserNameByUuid(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_always_returns_empty_if_map_argument_is_empty() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_name_of_user_from_map_argument() {
    Set<UserDto> users = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> UserTesting.newUserDto().setLogin("user" + i))
      .collect(Collectors.toSet());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(
      users.stream().collect(uniqueIndex(UserDto::getUuid)));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid("foo")).isEmpty();
    users
      .forEach(user -> assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).contains(user.getName()));
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getUserNameByUuid_returns_empty_if_user_has_null_name() {
    UserDto user = UserTesting.newUserDto().setLogin("user_noname").setName(null);

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(ImmutableMap.of(user.getUuid(), user));

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);
    assertThat(detailsSupplier.getUserNameByUuid(user.getUuid())).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_ISE_if_TreeRootHolder_is_not_initialized() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");

    detailsSupplier.getComponentNameByUuid("foo");
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_NPE_if_uuid_is_null() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getComponentNameByUuid(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_name_of_project_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getComponentNameByUuid("rootUuid")).contains("root");
    assertThat(detailsSupplier.getComponentNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_shortName_of_dir_and_file_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root")
      .addChildren(ReportComponent.builder(DIRECTORY, 2).setUuid("dir1Uuid").setName("dir1").setShortName("dir1_short")
        .addChildren(ReportComponent.builder(FILE, 21).setUuid("file21Uuid").setName("file21").setShortName("file21_short").build())
        .build())
      .addChildren(ReportComponent.builder(DIRECTORY, 3).setUuid("dir2Uuid").setName("dir2").setShortName("dir2_short")
        .addChildren(ReportComponent.builder(FILE, 31).setUuid("file31Uuid").setName("file31").setShortName("file31_short").build())
        .addChildren(ReportComponent.builder(FILE, 32).setUuid("file32Uuid").setName("file32").setShortName("file32_short").build())
        .build())
      .addChildren(ReportComponent.builder(FILE, 11).setUuid("file11Uuid").setName("file11").setShortName("file11_short").build())
      .build());
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    Stream.of("dir1", "dir2", "file11", "file21", "file31", "file32")
      .forEach(name -> {
        assertThat(detailsSupplier.getComponentNameByUuid(name + "Uuid")).contains(name + "_short");
        assertThat(detailsSupplier.getComponentNameByUuid(name)).isEmpty();
      });
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_ISE_if_TreeRootHolder_is_not_initialized() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Holder has not been initialized yet");

    detailsSupplier.getComponentNameByUuid("foo");
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_fails_with_NPE_if_uuid_is_null() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    detailsSupplier.getComponentNameByUuid(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_name_of_project_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root").build());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getComponentNameByUuid("rootUuid")).contains("root");
    assertThat(detailsSupplier.getComponentNameByUuid("foo")).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getComponentNameByUuid_returns_shortName_of_dir_and_file_in_TreeRootHolder() {
    treeRootHolder.setRoot(ReportComponent.builder(PROJECT, 1).setUuid("rootUuid").setName("root")
      .addChildren(ReportComponent.builder(DIRECTORY, 2).setUuid("dir1Uuid").setName("dir1").setShortName("dir1_short")
        .addChildren(ReportComponent.builder(FILE, 21).setUuid("file21Uuid").setName("file21").setShortName("file21_short").build())
        .build())
      .addChildren(ReportComponent.builder(DIRECTORY, 3).setUuid("dir2Uuid").setName("dir2").setShortName("dir2_short")
        .addChildren(ReportComponent.builder(FILE, 31).setUuid("file31Uuid").setName("file31").setShortName("file31_short").build())
        .addChildren(ReportComponent.builder(FILE, 32).setUuid("file32Uuid").setName("file32").setShortName("file32_short").build())
        .build())
      .addChildren(ReportComponent.builder(FILE, 11).setUuid("file11Uuid").setName("file11").setShortName("file11_short").build())
      .build());

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    Stream.of("dir1", "dir2", "file11", "file21", "file31", "file32")
      .forEach(name -> {
        assertThat(detailsSupplier.getComponentNameByUuid(name + "Uuid")).contains(name + "_short");
        assertThat(detailsSupplier.getComponentNameByUuid(name)).isEmpty();
      });
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_fails_with_NPE_if_ruleKey_is_null() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("ruleKey can't be null");

    detailsSupplier.getRuleDefinitionByRuleKey(null);
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_always_returns_empty_if_RuleRepository_is_empty() {
    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("foo", "bar"))).isEmpty();
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("bar", "foo"))).isEmpty();
  }

  @Test
  public void newMyNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_returns_name_and_language_from_RuleRepository() {
    RuleKey rulekey1 = RuleKey.of("foo", "bar");
    RuleKey rulekey2 = RuleKey.of("foo", "donut");
    RuleKey rulekey3 = RuleKey.of("no", "language");
    DumbRule rule1 = ruleRepository.add(rulekey1).setName("rule1").setLanguage("lang1");
    DumbRule rule2 = ruleRepository.add(rulekey2).setName("rule2").setLanguage("lang2");
    DumbRule rule3 = ruleRepository.add(rulekey3).setName("rule3");

    MyNewIssuesNotification underTest = this.underTest.newMyNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey1))
      .contains(new RuleDefinition(rule1.getName(), rule1.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey2))
      .contains(new RuleDefinition(rule2.getName(), rule2.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey3))
      .contains(new RuleDefinition(rule3.getName(), null));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("donut", "foo")))
      .isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_fails_with_NPE_if_ruleKey_is_null() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("ruleKey can't be null");

    detailsSupplier.getRuleDefinitionByRuleKey(null);
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_always_returns_empty_if_RuleRepository_is_empty() {
    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("foo", "bar"))).isEmpty();
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("bar", "foo"))).isEmpty();
  }

  @Test
  public void newNewIssuesNotification_DetailsSupplier_getRuleDefinitionByRuleKey_returns_name_and_language_from_RuleRepository() {
    RuleKey rulekey1 = RuleKey.of("foo", "bar");
    RuleKey rulekey2 = RuleKey.of("foo", "donut");
    RuleKey rulekey3 = RuleKey.of("no", "language");
    DumbRule rule1 = ruleRepository.add(rulekey1).setName("rule1").setLanguage("lang1");
    DumbRule rule2 = ruleRepository.add(rulekey2).setName("rule2").setLanguage("lang2");
    DumbRule rule3 = ruleRepository.add(rulekey3).setName("rule3");

    NewIssuesNotification underTest = this.underTest.newNewIssuesNotification(emptyMap());

    DetailsSupplier detailsSupplier = readDetailsSupplier(underTest);

    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey1))
      .contains(new RuleDefinition(rule1.getName(), rule1.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey2))
      .contains(new RuleDefinition(rule2.getName(), rule2.getLanguage()));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(rulekey3))
      .contains(new RuleDefinition(rule3.getName(), null));
    assertThat(detailsSupplier.getRuleDefinitionByRuleKey(RuleKey.of("donut", "foo")))
      .isEmpty();
  }

  private static Durations readDurationsField(NewIssuesNotification notification) {
    return readField(notification, "durations");
  }

  private static Durations readField(NewIssuesNotification notification, String fieldName) {
    try {
      Field durationsField = NewIssuesNotification.class.getDeclaredField(fieldName);
      durationsField.setAccessible(true);
      Object o = durationsField.get(notification);
      return (Durations) o;
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static DetailsSupplier readDetailsSupplier(NewIssuesNotification notification) {
    try {
      Field durationsField = NewIssuesNotification.class.getDeclaredField("detailsSupplier");
      durationsField.setAccessible(true);
      return (DetailsSupplier) durationsField.get(notification);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }
}
