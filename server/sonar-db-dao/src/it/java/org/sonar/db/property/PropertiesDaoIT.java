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
package org.sonar.db.property;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;
import static org.sonar.db.property.PropertyTesting.newUserPropertyDto;

class PropertiesDaoIT {

  private static final String VALUE_SIZE_4000 = String.format("%1$4000.4000s", "*");
  private static final String VALUE_SIZE_4001 = VALUE_SIZE_4000 + "P";
  private static final long INITIAL_DATE = 1_444_000L;

  private final AlwaysIncreasingSystem2 system2 = new AlwaysIncreasingSystem2(INITIAL_DATE, 1);
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2, auditPersister);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession session = db.getSession();
  private final PropertiesDao underTest = db.getDbClient().propertiesDao();

  @BeforeEach
  void setup() {
    when(auditPersister.isTrackedProperty(anyString())).thenReturn(true);
  }

  @Test
  void hasNotificationSubscribers() {
    UserDto user1 = db.users().insertUser(u -> u.setLogin("user1"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("user2"));
    String projectUuid = db.components().insertPrivateProject().getProjectDto().getUuid();
    String projectKey = secure().nextAlphabetic(4);
    String projectName = secure().nextAlphabetic(4);


    // global subscription
    insertProperty("notification.DispatcherWithGlobalSubscribers.Email", "true", null,
      user2.getUuid(), user2.getLogin(), null, null);
    // project subscription
    insertProperty("notification.DispatcherWithProjectSubscribers.Email", "true", projectUuid, user1.getUuid(),
      user1.getLogin(), projectKey, projectName);
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", "uuid56", user1.getUuid(),
      user1.getLogin(), projectKey, projectName);
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", projectUuid, user1.getUuid(),
      user1.getLogin(), projectKey, projectName);
    // global subscription
    insertProperty("notification.DispatcherWithGlobalAndProjectSubscribers.Email", "true", null, user2.getUuid(),
      user2.getLogin(), null, null);

    // Nobody is subscribed
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers(projectUuid, singletonList("NotSexyDispatcher")))
      .isFalse();

    // Global subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers(projectUuid, singletonList("DispatcherWithGlobalSubscribers")))
      .isTrue();

    // Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers(projectUuid, singletonList("DispatcherWithProjectSubscribers")))
      .isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList("DispatcherWithProjectSubscribers")))
      .isFalse();

    // Global + Project subscribers
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers(projectUuid, singletonList(
      "DispatcherWithGlobalAndProjectSubscribers")))
      .isTrue();
    assertThat(underTest.hasProjectNotificationSubscribersForDispatchers("PROJECT_B", singletonList(
      "DispatcherWithGlobalAndProjectSubscribers")))
      .isTrue();
  }

  @Test
  void findEmailRecipientsForNotification_returns_empty_on_empty_properties_table() {
    db.users().insertUser();
    String dispatcherKey = secure().nextAlphabetic(5);
    String channelKey = secure().nextAlphabetic(6);
    String projectKey = secure().nextAlphabetic(7);

    Set<EmailSubscriberDto> subscribers = underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey,
      projectKey);

    assertThat(subscribers).isEmpty();
  }

  @Test
  void findEmailRecipientsForNotification_with_logins_returns_empty_on_empty_properties_table() {
    db.users().insertUser();
    String dispatcherKey = secure().nextAlphabetic(5);
    String channelKey = secure().nextAlphabetic(6);
    String projectKey = secure().nextAlphabetic(7);
    Set<String> logins = of("user1", "user2");

    Set<EmailSubscriberDto> subscribers = underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey,
      projectKey, logins);

    assertThat(subscribers).isEmpty();
  }

  @Test
  void findEmailRecipientsForNotification_finds_only_globally_subscribed_users_if_projectKey_is_null() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    ProjectDto project = insertPrivateProject("PROJECT_A");
    String dispatcherKey = secure().nextAlphabetic(5);
    String otherDispatcherKey = secure().nextAlphabetic(6);
    String channelKey = secure().nextAlphabetic(7);
    String otherChannelKey = secure().nextAlphabetic(8);
    // user1 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    // user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 subscribed on project only
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user3.getUuid(), user3.getLogin(),
      project.getKey(), project.getName());
    // user4 did not subscribe
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "false", project.getUuid(), user4.getUuid(), user4.getLogin(),
      project.getKey(), project.getName());

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null))
      .containsOnly(EmailSubscriberDto.create("user1", true, emailOf("user1")), EmailSubscriberDto.create("user2", true, emailOf("user2")));

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, otherChannelKey, null))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), otherDispatcherKey, channelKey, null))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), channelKey, dispatcherKey, null))
      .isEmpty();
  }

  @Test
  void findEmailRecipientsForNotification_with_logins_finds_only_globally_subscribed_specified_users_if_projectKey_is_null() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    ProjectDto project = insertPrivateProject("PROJECT_A");
    String dispatcherKey = secure().nextAlphabetic(5);
    String otherDispatcherKey = secure().nextAlphabetic(6);
    String channelKey = secure().nextAlphabetic(7);
    String otherChannelKey = secure().nextAlphabetic(8);
    // user1 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    // user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 subscribed on project only
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user3.getUuid(), user3.getLogin(),
      project.getKey(), project.getName());
    // user4 did not subscribe
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "false", project.getUuid(), user4.getUuid(), user4.getLogin(),
      project.getKey(), project.getName());
    Set<String> allLogins = of("user1", "user2", "user3", "user4");

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, allLogins))
      .containsOnly(EmailSubscriberDto.create("user1", true, emailOf("user1")), EmailSubscriberDto.create("user2", true, emailOf("user2")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, of("user1", "user2")))
      .containsOnly(EmailSubscriberDto.create("user1", true, emailOf("user1")), EmailSubscriberDto.create("user2", true, emailOf("user2")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, of("user2")))
      .containsOnly(EmailSubscriberDto.create("user2", true, emailOf("user2")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, of("user1")))
      .containsOnly(EmailSubscriberDto.create("user1", true, emailOf("user1")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, of()))
      .isEmpty();

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, otherChannelKey, null, allLogins))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), otherDispatcherKey, channelKey, null, allLogins))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), channelKey, dispatcherKey, null, allLogins))
      .isEmpty();
  }

  @Test
  void selectEntityPropertyByKeyAndUserUuid_shouldFindPortfolioProperties() {
    PortfolioDto portfolio = db.components().insertPrivatePortfolioDto();
    String uuid1 = insertProperty("key", "value1", portfolio.getUuid(), "user1", null, null, null);
    String uuid2 = insertProperty("key", "value2", portfolio.getUuid(), "user2", null, null, null);
    String uuid3 = insertProperty("key2", "value3", portfolio.getUuid(), "user1", null, null, null);

    List<PropertyDto> property = underTest.selectEntityPropertyByKeyAndUserUuid(db.getSession(), "key", "user1");

    assertThat(property)
      .extracting(PropertyDto::getValue, PropertyDto::getEntityUuid, PropertyDto::getKey)
      .containsOnly(tuple("value1", portfolio.getUuid(), "key"));
  }

  @Test
  void selectEntityPropertyByKeyAndUserUuid_shouldFindProjectAndAppProperties() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    String uuid1 = insertProperty("key", "value1", project.getUuid(), "user1", null, null, null);
    String uuid2 = insertProperty("key", "value2", project.getUuid(), "user2", null, null, null);
    String uuid3 = insertProperty("key2", "value3", project.getUuid(), "user1", null, null, null);

    List<PropertyDto> property = underTest.selectEntityPropertyByKeyAndUserUuid(db.getSession(), "key", "user1");

    assertThat(property)
      .extracting(PropertyDto::getValue, PropertyDto::getEntityUuid, PropertyDto::getKey)
      .containsOnly(tuple("value1", project.getUuid(), "key"));
  }

  @Test
  void findEmailRecipientsForNotification_finds_global_and_project_subscribed_users_when_projectKey_is_non_null() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    String projectKey = secure().nextAlphabetic(3);
    String otherProjectKey = secure().nextAlphabetic(4);
    ProjectDto project = insertPrivateProject(projectKey);
    String dispatcherKey = secure().nextAlphabetic(5);
    String otherDispatcherKey = secure().nextAlphabetic(6);
    String channelKey = secure().nextAlphabetic(7);
    String otherChannelKey = secure().nextAlphabetic(8);
    // user1 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    // user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 subscribed on project only
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user3.getUuid(), user3.getLogin(),
      project.getKey(), project.getName());
    // user4 did not subscribe
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "false", project.getUuid(), user4.getUuid(), user4.getLogin(),
      project.getKey(), project.getName());

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user2", true, emailOf("user2")), EmailSubscriberDto.create("user2", false, "user2@foo"),
        EmailSubscriberDto.create("user3", false, emailOf("user3")));

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, otherProjectKey))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user2", true, emailOf("user2")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, otherChannelKey, otherProjectKey))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), otherDispatcherKey, channelKey, otherProjectKey))
      .isEmpty();
  }

  @Test
  void findEmailRecipientsForNotification_with_logins_finds_global_and_project_subscribed_specified_users_when_projectKey_is_non_null() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    String projectKey = secure().nextAlphabetic(3);
    String otherProjectKey = secure().nextAlphabetic(4);
    ProjectDto project = insertPrivateProject(projectKey);
    String dispatcherKey = secure().nextAlphabetic(5);
    String otherDispatcherKey = secure().nextAlphabetic(6);
    String channelKey = secure().nextAlphabetic(7);
    String otherChannelKey = secure().nextAlphabetic(8);
    // user1 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    // user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 subscribed on project only
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user3.getUuid(), user3.getLogin(),
      project.getKey(), project.getName());
    // user4 did not subscribe
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "false", project.getUuid(), user4.getUuid(), user4.getLogin(),
      project.getKey(), project.getName());
    Set<String> allLogins = of("user1", "user2", "user3", "user4");

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, allLogins))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user2", true, emailOf("user2")), EmailSubscriberDto.create("user2", false, "user2@foo"),
        EmailSubscriberDto.create("user3", false, emailOf("user3")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, of("user1")))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, of("user2")))
      .containsOnly(
        EmailSubscriberDto.create("user2", true, emailOf("user2")), EmailSubscriberDto.create("user2", false, "user2@foo"));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, of("user3")))
      .containsOnly(EmailSubscriberDto.create("user3", false, emailOf("user3")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, of()))
      .isEmpty();

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, otherProjectKey, allLogins))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user2", true, emailOf("user2")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, otherChannelKey, otherProjectKey, allLogins))
      .isEmpty();
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), otherDispatcherKey, channelKey, otherProjectKey, allLogins))
      .isEmpty();
  }

  @Test
  void findEmailRecipientsForNotification_ignores_subscribed_users_without_email() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(noEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(noEmail("user4"));
    String projectKey = secure().nextAlphabetic(3);
    ProjectDto project = insertPrivateProject(projectKey);
    String dispatcherKey = secure().nextAlphabetic(4);
    String channelKey = secure().nextAlphabetic(5);
    // user1 and user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user1.getUuid(), user1.getLogin(),
      project.getKey(), project.getName());
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 and user4 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user3.getUuid(), user3.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user4.getUuid(), user4.getLogin(),
      null, null);

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")), EmailSubscriberDto.create("user1", false, emailOf("user1")),
        EmailSubscriberDto.create("user3", true, emailOf("user3")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user3", true, emailOf("user3")));
  }

  @Test
  void findEmailRecipientsForNotification_with_logins_ignores_subscribed_users_without_email() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    UserDto user2 = db.users().insertUser(noEmail("user2"));
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    UserDto user4 = db.users().insertUser(noEmail("user4"));
    Set<String> allLogins = of("user1", "user2", "user3");
    String projectKey = secure().nextAlphabetic(3);
    ProjectDto project = insertPrivateProject(projectKey);
    String dispatcherKey = secure().nextAlphabetic(4);
    String channelKey = secure().nextAlphabetic(5);
    // user1 and user2 subscribed on project and globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user1.getUuid(), user1.getLogin(),
      null, null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user1.getUuid(), user1.getLogin(),
      project.getKey(), project.getName());
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user2.getUuid(), user2.getLogin(),
      project.getKey(), null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", project.getUuid(), user2.getUuid(), user2.getLogin(),
      project.getKey(), project.getName());
    // user3 and user4 subscribed only globally
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user3.getUuid(), user3.getLogin(),
      project.getKey(), null);
    insertProperty(propertyKeyOf(dispatcherKey, channelKey), "true", null, user4.getUuid(), user4.getLogin(),
      null, null);

    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, projectKey, allLogins))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")), EmailSubscriberDto.create("user1", false, emailOf("user1")),
        EmailSubscriberDto.create("user3", true, emailOf("user3")));
    assertThat(underTest.findEmailSubscribersForNotification(db.getSession(), dispatcherKey, channelKey, null, allLogins))
      .containsOnly(
        EmailSubscriberDto.create("user1", true, emailOf("user1")),
        EmailSubscriberDto.create("user3", true, emailOf("user3")));
  }

  @Test
  void selectGlobalProperties() {
    // global
    insertProperty("global.one", "one", null, null, null, null, null);
    insertProperty("global.two", "two", null, null, null, null, null);

    List<PropertyDto> properties = underTest.selectGlobalProperties(db.getSession());
    assertThat(properties)
      .hasSize(2);

    assertThat(findByKey(properties, "global.one"))
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly("global.one", null, null, "one");

    assertThat(findByKey(properties, "global.two"))
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly("global.two", null, null, "two");
  }

  @ParameterizedTest
  @MethodSource("allValuesForSelect")
  void selectGlobalProperties_supports_all_values(String dbValue, String expected) {
    insertProperty("global.one", dbValue, null, null, null, null, null);

    List<PropertyDto> dtos = underTest.selectGlobalProperties(db.getSession());
    assertThat(dtos)
      .hasSize(1);

    assertThat(dtos.iterator().next())
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly("global.one", null, null, expected);
  }

  @Test
  void selectGlobalProperty() {
    // global
    insertProperty("global.one", "one", null, null, null, null, null);
    insertProperty("global.two", "two", null, null, null, null, null);
    // project
    insertProperty("project.one", "one", "uuid10", null, null, "component", "component");
    // user
    insertProperty("user.one", "one", null, "100", "login", null, null);

    assertThat(underTest.selectGlobalProperty("global.one"))
      .extracting(PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly(null, null, "one");

    assertThat(underTest.selectGlobalProperty("project.one")).isNull();
    assertThat(underTest.selectGlobalProperty("user.one")).isNull();
    assertThat(underTest.selectGlobalProperty("unexisting")).isNull();
  }

  @ParameterizedTest
  @MethodSource("allValuesForSelect")
  void selectGlobalProperty_supports_all_values(String dbValue, String expected) {
    insertProperty("global.one", dbValue, null, null, null, null, null);

    assertThat(underTest.selectGlobalProperty("global.one"))
      .extracting(PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly(null, null, expected);
  }

  @ParameterizedTest
  @MethodSource("allValuesForSelect")
  void selectProjectProperties_supports_all_values(String dbValue, String expected) {
    ProjectDto projectDto = insertPrivateProject("A");
    insertProperty("project.one", dbValue, projectDto.getUuid(), null, null, projectDto.getKey(), projectDto.getName());

    List<PropertyDto> dtos = underTest.selectEntityProperties(db.getSession(), projectDto.getUuid());
    assertThat(dtos).hasSize(1);

    assertThat(dtos.iterator().next())
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid, PropertyDto::getValue)
      .containsExactly("project.one", projectDto.getUuid(), expected);
  }

  private static Object[][] allValuesForSelect() {
    return new Object[][]{
      {null, ""},
      {"", ""},
      {"some value", "some value"},
      {VALUE_SIZE_4000, VALUE_SIZE_4000},
      {VALUE_SIZE_4001, VALUE_SIZE_4001}
    };
  }

  @Test
  void selectProjectProperty() {
    insertProperty("project.one", "one", "uuid10", null, null, "component", "component");

    PropertyDto property = underTest.selectProjectProperty(db.getSession(), "uuid10", "project.one");

    assertThat(property)
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid, PropertyDto::getUserUuid, PropertyDto::getValue)
      .containsExactly("project.one", "uuid10", null, "one");

    assertThat(underTest.selectProjectProperty("uuid10", "project.one"))
      .isPresent()
      .contains(property);
  }

  @Test
  void select_by_query() {
    // global
    insertProperty("global.one", "one", null, null, null, null, null);
    insertProperty("global.two", "two", null, null, null, null, null);
    // struts
    insertProperty("struts.one", "one", "uuid10", null, null, "component", "component");
    // commons
    insertProperty("commonslang.one", "one", "uuid11", null, null, "component", "component");
    // user
    insertProperty("user.one", "one", null, "100", "login", null, null);
    insertProperty("user.two", "two", "uuid10", "100", "login", "component", "component");
    // other
    insertProperty("other.one", "one", "uuid12", null, null, "component", "component");

    List<PropertyDto> results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.two").setEntityUuid("uuid10")
      .setUserUuid("100").build(), db.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("two");

    results = underTest.selectByQuery(PropertyQuery.builder().setKey("user.one").setUserUuid("100").build(), db.getSession());
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getValue()).isEqualTo("one");
  }

  @Test
  void select_global_properties_by_keys() {
    insertPrivateProject("A");
    UserDto user = db.users().insertUser(u -> u.setLogin("B"));

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperty(key, "value", null, null, null, null, null);
    insertProperty(key, "value", "uuid10", null, null, "component", "component");
    insertProperty(key, "value", null, user.getUuid(), user.getLogin(), null, null);
    insertProperty(anotherKey, "value", null, null, null, null, null);

    insertProperty("key1", "value", null, null, null, null, null);
    insertProperty("key2", "value", null, null, null, null, null);
    insertProperty("key3", "value", null, null, null, null, null);

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key)))
      .extracting("key")
      .containsExactly(key);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey)))
      .extracting("key")
      .containsExactly(key, anotherKey);
    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet(key, anotherKey, "unknown")))
      .extracting("key")
      .containsExactly(key, anotherKey);

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet("key2", "key1", "key3")))
      .extracting("key")
      .containsExactly("key1", "key2", "key3");

    assertThat(underTest.selectGlobalPropertiesByKeys(session, newHashSet("unknown")))
      .isEmpty();
  }

  @Test
  void select_properties_by_keys_and_component_ids() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser();

    String key = "key";
    String anotherKey = "anotherKey";
    insertProperties(null, null, null, newGlobalPropertyDto().setKey(key));
    insertProperties(null, project.getKey(), project.getName(), newComponentPropertyDto(project).setKey(key));
    insertProperties(null, project2.getKey(), project2.getName(), newComponentPropertyDto(project2).setKey(key),
      newComponentPropertyDto(project2).setKey(anotherKey));
    insertProperties(user.getLogin(), null, null, newUserPropertyDto(user).setKey(key));

    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet(key), newHashSet(project.getUuid())))
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid).containsOnly(tuple(key, project.getUuid()));
    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet(key), newHashSet(project.getUuid(), project2.getUuid())))
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid).containsOnly(
        tuple(key, project.getUuid()),
        tuple(key, project2.getUuid()));
    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet(key, anotherKey), newHashSet(project.getUuid(),
      project2.getUuid())))
      .extracting(PropertyDto::getKey, PropertyDto::getEntityUuid).containsOnly(
        tuple(key, project.getUuid()),
        tuple(key, project2.getUuid()),
        tuple(anotherKey, project2.getUuid()));

    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet("unknown"), newHashSet(project.getUuid()))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet("key"), newHashSet("uuid123456789"))).isEmpty();
    assertThat(underTest.selectPropertiesByKeysAndEntityUuids(session, newHashSet("unknown"), newHashSet("uuid123456789"))).isEmpty();
  }

  @Test
  void select_by_key_and_matching_value() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.properties().insertProperties(null, project1.getKey(), project1.getName(), project1.getQualifier(), newComponentPropertyDto("key",
      "value", project1));
    db.properties().insertProperties(null, project2.getKey(), project2.getName(), project2.getQualifier(), newComponentPropertyDto("key",
      "value", project2));
    db.properties().insertProperties(null, null, null, null, newGlobalPropertyDto("key", "value"));
    db.properties().insertProperties(null, project1.getKey(), project1.getName(), project1.getQualifier(), newComponentPropertyDto(
      "another key", "value", project1));

    assertThat(underTest.selectByKeyAndMatchingValue(db.getSession(), "key", "value"))
      .extracting(PropertyDto::getValue, PropertyDto::getEntityUuid)
      .containsExactlyInAnyOrder(
        tuple("value", project1.getUuid()),
        tuple("value", project2.getUuid()),
        tuple("value", null));
  }

  @Test
  void saveProperty_inserts_global_properties_when_they_do_not_exist_in_db() {
    underTest.saveProperty(new PropertyDto().setKey("global.null").setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("global.empty").setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("global.text").setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("global.4000").setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("global.clob").setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("global.null")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE);
    assertThatPropertiesRow("global.empty")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE + 1);
    assertThatPropertiesRow("global.text")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .hasTextValue("some text")
      .hasCreatedAt(INITIAL_DATE + 2);
    assertThatPropertiesRow("global.4000")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(INITIAL_DATE + 3);
    assertThatPropertiesRow("global.clob")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(INITIAL_DATE + 4);
  }

  @Test
  void saveProperty_inserts_component_properties_when_they_do_not_exist_in_db() {
    String componentUuid = "uuid12";
    underTest.saveProperty(new PropertyDto().setKey("component.null").setEntityUuid(componentUuid).setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("component.empty").setEntityUuid(componentUuid).setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("component.text").setEntityUuid(componentUuid).setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("component.4000").setEntityUuid(componentUuid).setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("component.clob").setEntityUuid(componentUuid).setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("component.null")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE);
    assertThatPropertiesRow("component.empty")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE + 1);
    assertThatPropertiesRow("component.text")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .hasTextValue("some text")
      .hasCreatedAt(INITIAL_DATE + 2);
    assertThatPropertiesRow("component.4000")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(INITIAL_DATE + 3);
    assertThatPropertiesRow("component.clob")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(INITIAL_DATE + 4);
  }

  @Test
  void saveProperty_inserts_user_properties_when_they_do_not_exist_in_db() {
    String userUuid = "uuid-100";
    underTest.saveProperty(new PropertyDto().setKey("user.null").setUserUuid(userUuid).setValue(null));
    underTest.saveProperty(new PropertyDto().setKey("user.empty").setUserUuid(userUuid).setValue(""));
    underTest.saveProperty(new PropertyDto().setKey("user.text").setUserUuid(userUuid).setValue("some text"));
    underTest.saveProperty(new PropertyDto().setKey("user.4000").setUserUuid(userUuid).setValue(VALUE_SIZE_4000));
    underTest.saveProperty(new PropertyDto().setKey("user.clob").setUserUuid(userUuid).setValue(VALUE_SIZE_4001));

    assertThatPropertiesRow("user.null")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE);
    assertThatPropertiesRow("user.empty")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .isEmpty()
      .hasCreatedAt(INITIAL_DATE + 1);
    assertThatPropertiesRow("user.text")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .hasTextValue("some text")
      .hasCreatedAt(INITIAL_DATE + 2);
    assertThatPropertiesRow("user.4000")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .hasTextValue(VALUE_SIZE_4000)
      .hasCreatedAt(INITIAL_DATE + 3);
    assertThatPropertiesRow("user.clob")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .hasClobValue(VALUE_SIZE_4001)
      .hasCreatedAt(INITIAL_DATE + 4);
  }

  @ParameterizedTest
  @MethodSource("valueUpdatesDataProvider")
  void saveProperty_deletes_then_inserts_global_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) {
    String uuid = insertProperty("global", oldValue, null, null, null, null, null);

    underTest.saveProperty(new PropertyDto().setKey("global").setValue(newValue));

    assertThatPropertiesRowByUuid(uuid)
      .doesNotExist();

    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .hasCreatedAt(INITIAL_DATE + 1);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  @ParameterizedTest
  @MethodSource("valueUpdatesDataProvider")
  void saveProperty_deletes_then_inserts_component_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) {
    String componentUuid = "uuid999";
    String uuid = insertProperty("global", oldValue, componentUuid, null, null, "component", "component");

    underTest.saveProperty(new PropertyDto().setKey("global").setEntityUuid(componentUuid).setValue(newValue));

    assertThatPropertiesRowByUuid(uuid)
      .doesNotExist();
    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasComponentUuid(componentUuid)
      .hasNoUserUuid()
      .hasCreatedAt(INITIAL_DATE + 1);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  @ParameterizedTest
  @MethodSource("valueUpdatesDataProvider")
  void saveProperty_deletes_then_inserts_user_properties_when_they_exist_in_db(@Nullable String oldValue, @Nullable String newValue) {
    String userUuid = "uuid-90";
    String uuid = insertProperty("global", oldValue, null, userUuid, "login", null, null);

    underTest.saveProperty(new PropertyDto().setKey("global").setUserUuid(userUuid).setValue(newValue));

    assertThatPropertiesRowByUuid(uuid)
      .doesNotExist();

    PropertiesRowAssert propertiesRowAssert = assertThatPropertiesRow("global")
      .hasNoComponentUuid()
      .hasUserUuid(userUuid)
      .hasCreatedAt(INITIAL_DATE + 1);
    if (newValue == null || newValue.isEmpty()) {
      propertiesRowAssert.isEmpty();
    } else if (newValue.length() > 4000) {
      propertiesRowAssert.hasClobValue(newValue);
    } else {
      propertiesRowAssert.hasTextValue(newValue);
    }
  }

  static Object[][] valueUpdatesDataProvider() {
    return new Object[][]{
      {null, null},
      {null, ""},
      {null, "some value"},
      {null, VALUE_SIZE_4000},
      {null, VALUE_SIZE_4001},
      {"", null},
      {"", ""},
      {"", "some value"},
      {"", VALUE_SIZE_4000},
      {"", VALUE_SIZE_4001},
      {"a value", null},
      {"a value", ""},
      {"a value", "a value"},
      {"a value", "some value"},
      {"a value", VALUE_SIZE_4000},
      {"a value", VALUE_SIZE_4001},
      {VALUE_SIZE_4000, null},
      {VALUE_SIZE_4000, ""},
      {VALUE_SIZE_4000, "a value"},
      {VALUE_SIZE_4000, VALUE_SIZE_4000},
      {VALUE_SIZE_4000, VALUE_SIZE_4000.substring(1) + "a"},
      {VALUE_SIZE_4000, VALUE_SIZE_4001},
      {VALUE_SIZE_4001, null},
      {VALUE_SIZE_4001, ""},
      {VALUE_SIZE_4001, "a value"},
      {VALUE_SIZE_4001, VALUE_SIZE_4000},
      {VALUE_SIZE_4001, VALUE_SIZE_4001},
      {VALUE_SIZE_4001, VALUE_SIZE_4001 + "dfsdfs"},
    };
  }

  @Test
  void deleteGlobalProperty() {
    // global
    String uuid1 = insertProperty("global.key", "new_global", null, null, null, null, null);
    String uuid2 = insertProperty("to_be_deleted", "xxx", null, null, null, null, null);
    // project - do not delete this project property that has the same key
    String uuid3 = insertProperty("to_be_deleted", "new_project", "to_be_deleted", null, null,
      "component", "component");
    // user
    String uuid4 = insertProperty("user.key", "new_user", null, "100", "login", null, null);

    underTest.deleteGlobalProperty("to_be_deleted", db.getSession());
    db.getSession().commit();

    assertThatPropertiesRowByUuid(uuid1)
      .hasKey("global.key")
      .hasNoUserUuid()
      .hasNoComponentUuid()
      .hasTextValue("new_global");
    assertThatPropertiesRowByUuid(uuid2)
      .doesNotExist();
    assertThatPropertiesRow("to_be_deleted", null, null)
      .doesNotExist();
    assertThatPropertiesRowByUuid(uuid3)
      .hasKey("to_be_deleted")
      .hasComponentUuid("to_be_deleted")
      .hasNoUserUuid()
      .hasTextValue("new_project");
    assertThatPropertiesRowByUuid(uuid4)
      .hasKey("user.key")
      .hasNoComponentUuid()
      .hasUserUuid("100")
      .hasTextValue("new_user");
  }

  @Test
  void delete_by_key_and_value() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto anotherProject = db.components().insertPrivateProject().getMainBranchComponent();
    insertProperty("KEY", "VALUE", null, null, null, null, null);
    insertProperty("KEY", "VALUE", project.uuid(), null, null, project.getKey(), project.name());
    insertProperty("KEY", "VALUE", null, "100", "login", null, null);
    insertProperty("KEY", "VALUE", project.uuid(), "100", "login", project.getKey(), project.name());
    insertProperty("KEY", "VALUE", anotherProject.uuid(), null, null, anotherProject.getKey(), anotherProject.name());
    // Should not be removed
    insertProperty("KEY", "ANOTHER_VALUE", null, null, null, null, null);
    insertProperty("ANOTHER_KEY", "VALUE", project.uuid(), "100", "login", project.getKey(), project.name());

    underTest.deleteByKeyAndValue(session, "KEY", "VALUE");
    db.commit();

    assertThat(db.select("select prop_key as \"key\", text_value as \"value\", entity_uuid as \"projectUuid\", user_uuid as \"userUuid\" " +
      "from properties"))
      .extracting((row) -> row.get("key"), (row) -> row.get("value"), (row) -> row.get("projectUuid"), (row) -> row.get("userUuid"))
      .containsOnly(tuple("KEY", "ANOTHER_VALUE", null, null), tuple("ANOTHER_KEY", "VALUE", project.uuid(), "100"));
  }

  private static Map<String, String> mapOf(String... values) {
    // use LinkedHashMap to keep order of array
    Map<String, String> res = new LinkedHashMap<>(values.length / 2);
    Iterator<String> iterator = Arrays.asList(values).iterator();
    while (iterator.hasNext()) {
      res.put(iterator.next(), iterator.next());
    }
    return res;
  }

  @Test
  void renamePropertyKey_updates_global_component_and_user_properties() {
    String uuid1 = insertProperty("foo", "bar", null, null, null, null, null);
    String uuid2 = insertProperty("old_name", "doc1", null, null, null, null, null);
    String uuid3 = insertProperty("old_name", "doc2", "15", null, null, "component", "component");
    String uuid4 = insertProperty("old_name", "doc3", "16", null, null, "component", "component");
    String uuid5 = insertProperty("old_name", "doc4", null, "100", "login", null, null);
    String uuid6 = insertProperty("old_name", "doc5", null, "101", "login", null, null);

    underTest.renamePropertyKey("old_name", "new_name");

    assertThatPropertiesRowByUuid(uuid1)
      .hasKey("foo")
      .hasNoUserUuid()
      .hasNoComponentUuid()
      .hasTextValue("bar")
      .hasCreatedAt(INITIAL_DATE);
    assertThatPropertiesRowByUuid(uuid2)
      .hasKey("new_name")
      .hasNoComponentUuid()
      .hasNoUserUuid()
      .hasTextValue("doc1")
      .hasCreatedAt(INITIAL_DATE + 1);
    assertThatPropertiesRowByUuid(uuid3)
      .hasKey("new_name")
      .hasComponentUuid("15")
      .hasNoUserUuid()
      .hasTextValue("doc2")
      .hasCreatedAt(INITIAL_DATE + 2);
    assertThatPropertiesRowByUuid(uuid4)
      .hasKey("new_name")
      .hasComponentUuid("16")
      .hasNoUserUuid()
      .hasTextValue("doc3")
      .hasCreatedAt(INITIAL_DATE + 3);
    assertThatPropertiesRowByUuid(uuid5)
      .hasKey("new_name")
      .hasNoComponentUuid()
      .hasUserUuid("100")
      .hasTextValue("doc4")
      .hasCreatedAt(INITIAL_DATE + 4);
    assertThatPropertiesRowByUuid(uuid6)
      .hasKey("new_name")
      .hasNoComponentUuid()
      .hasUserUuid("101")
      .hasTextValue("doc5")
      .hasCreatedAt(INITIAL_DATE + 5);
  }

  @Test
  void rename_to_same_key_has_no_effect() {
    String uuid = insertProperty("foo", "bar", null, null, null, null, null);

    assertThatPropertiesRowByUuid(uuid)
      .hasCreatedAt(INITIAL_DATE);

    underTest.renamePropertyKey("foo", "foo");

    assertThatPropertiesRowByUuid(uuid)
      .hasKey("foo")
      .hasNoUserUuid()
      .hasNoComponentUuid()
      .hasTextValue("bar")
      .hasCreatedAt(INITIAL_DATE);
  }

  @Test
  void should_not_rename_with_empty_key() {
    assertThatThrownBy(() -> underTest.renamePropertyKey("foo", ""))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_not_rename_an_empty_key() {
    assertThatThrownBy(() -> underTest.renamePropertyKey(null, "foo"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void insert_shouldFail_whenPropertyAlreadyExists() {
    PropertiesMapper mapper = db.getSession().getSqlSession().getMapper(PropertiesMapper.class);

    mapper.insertAsText("uuid1", "key", null, null, "value", new Date().getTime());
    assertThatThrownBy(() -> {
      mapper.insertAsText("uuid2", "key", null, null, "value", new Date().getTime());
    }).hasCauseInstanceOf(SQLException.class);
  }

  @Test
  void insert_shouldFail_whenPropertyAlreadyExistsOnKeyAndUser() {
    PropertiesMapper mapper = db.getSession().getSqlSession().getMapper(PropertiesMapper.class);

    mapper.insertAsText("uuid3", "key", "user", null, "value", new Date().getTime());
    assertThatThrownBy(() -> mapper.insertAsText("uuid4", "key", "user", null, "value", new Date().getTime()))
      .hasCauseInstanceOf(SQLException.class);
  }

  @Test
  void insert_shouldFail_whenPropertyAlreadyExistsOnKeyAndUserAndEntity() {
    PropertiesMapper mapper = db.getSession().getSqlSession().getMapper(PropertiesMapper.class);

    mapper.insertAsText("uuid5", "key", "user", "entity", "value", new Date().getTime());
    assertThatThrownBy(() -> mapper.insertAsText("uuid6", "key", "user", "entity", "value", new Date().getTime()))
      .hasCauseInstanceOf(SQLException.class);

  }

  @CheckForNull
  private PropertyDto findByKey(List<PropertyDto> properties, String key) {
    for (PropertyDto property : properties) {
      if (key.equals(property.getKey())) {
        return property;
      }
    }
    return null;
  }

  private void insertProperties(@Nullable String userLogin, @Nullable String projectKey,
    @Nullable String projectName, PropertyDto... properties) {
    for (PropertyDto propertyDto : properties) {
      underTest.saveProperty(session, propertyDto, userLogin, projectKey, projectName, ComponentQualifiers.PROJECT);
    }
    session.commit();
  }

  private String insertProperty(String key, @Nullable String value, @Nullable String entityUuid, @Nullable String userUuid,
    @Nullable String userLogin, @Nullable String projectKey, @Nullable String projectName) {
    clearInvocations(auditPersister);
    PropertyDto dto = new PropertyDto().setKey(key)
      .setEntityUuid(entityUuid)
      .setUserUuid(userUuid)
      .setValue(value);
    boolean isNew = session.getMapper(PropertiesMapper.class).selectByKey(dto) == null;
    db.properties().insertProperty(dto, projectKey, projectName, ComponentQualifiers.PROJECT, userLogin);
    if (isNew) {
      verify(auditPersister).addProperty(any(), any(), anyBoolean());
    } else {
      verify(auditPersister).updateProperty(any(), any(), anyBoolean());
    }

    return (String) db.selectFirst(session, "select uuid as \"uuid\" from properties" +
      " where prop_key='" + key + "'" +
      " and user_uuid" + (userUuid == null ? " is null" : "='" + userUuid + "'") +
      " and entity_uuid" + (entityUuid == null ? " is null" : "='" + entityUuid + "'")).get("uuid");
  }

  private ProjectDto insertPrivateProject(String projectKey) {
    return db.components().insertPrivateProject(t -> t.setKey(projectKey)).getProjectDto();
  }

  private static Consumer<UserDto> withEmail(String login) {
    return u -> u.setLogin(login).setEmail(emailOf(login));
  }

  private static String emailOf(String login) {
    return login + "@foo";
  }

  private static Consumer<UserDto> noEmail(String login) {
    return u -> u.setLogin(login).setEmail(null);
  }

  private static String propertyKeyOf(String dispatcherKey, String channelKey) {
    return String.format("notification.%s.%s", dispatcherKey, channelKey);
  }

  private PropertiesRowAssert assertThatPropertiesRow(String key, @Nullable String userUuid, @Nullable String componentUuid) {
    return new PropertiesRowAssert(db, key, userUuid, componentUuid);
  }

  private PropertiesRowAssert assertThatPropertiesRow(String key) {
    return new PropertiesRowAssert(db, key);
  }

  private PropertiesRowAssert assertThatPropertiesRowByUuid(String uuid) {
    return PropertiesRowAssert.byUuid(db, uuid);
  }

}
