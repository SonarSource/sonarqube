/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.notification;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertiesDao;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NotificationServiceTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final PropertiesDao propertiesDao = mock(PropertiesDao.class);

  @Before
  public void wire_mocks() {
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
  }

  @Test
  public void deliverEmails_fails_with_IAE_if_type_of_collection_is_Notification() {
    NotificationHandler handler = getMockOfNotificationHandlerForType(Notification1.class);
    List<Notification> notifications = IntStream.range(0, 10)
      .mapToObj(i -> new Notification("i"))
      .toList();
    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[]{handler});

    assertThatThrownBy(() -> underTest.deliverEmails(notifications))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Type of notification objects must be a subtype of Notification");
  }

  @Test
  public void deliverEmails_collection_has_no_effect_if_no_handler_nor_dispatcher() {
    List<Notification> notifications = IntStream.range(0, 10)
      .mapToObj(i -> mock(Notification.class))
      .toList();
    NotificationService underTest = new NotificationService(dbClient);

    assertThat(underTest.deliverEmails(notifications)).isZero();
    verifyNoInteractions(dbClient);
  }

  @Test
  public void deliverEmails_collection_has_no_effect_if_no_handler() {
    NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
    List<Notification> notifications = IntStream.range(0, 10)
      .mapToObj(i -> mock(Notification.class))
      .toList();
    NotificationService underTest = new NotificationService(dbClient, new NotificationDispatcher[]{dispatcher});

    assertThat(underTest.deliverEmails(notifications)).isZero();
    verifyNoInteractions(dispatcher);
    verifyNoInteractions(dbClient);
  }

  @Test
  public void deliverEmails_collection_returns_0_if_collection_is_empty() {
    NotificationHandler<Notification1> handler1 = getMockOfNotificationHandlerForType(Notification1.class);
    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    NotificationService underTest = new NotificationService(dbClient,
      new NotificationHandler[]{handler1, handler2});

    assertThat(underTest.deliverEmails(Collections.emptyList())).isZero();
    verifyNoInteractions(handler1, handler2);
  }

  @Test
  public void deliverEmails_collection_returns_0_if_no_handler_for_the_notification_class() {
    NotificationHandler<Notification1> handler1 = getMockOfNotificationHandlerForType(Notification1.class);
    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    List<Notification1> notification1s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification1())
      .toList();
    List<Notification2> notification2s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification2())
      .toList();
    NotificationService noHandler = new NotificationService(dbClient);
    NotificationService onlyHandler1 = new NotificationService(dbClient, new NotificationHandler[]{handler1});
    NotificationService onlyHandler2 = new NotificationService(dbClient, new NotificationHandler[]{handler2});

    assertThat(noHandler.deliverEmails(notification1s)).isZero();
    assertThat(noHandler.deliverEmails(notification2s)).isZero();
    assertThat(onlyHandler1.deliverEmails(notification2s)).isZero();
    assertThat(onlyHandler2.deliverEmails(notification1s)).isZero();
    verify(handler1, times(0)).deliver(anyCollection());
    verify(handler2, times(0)).deliver(anyCollection());
  }

  @Test
  public void deliverEmails_collection_calls_deliver_method_of_handler_for_notification_class_and_returns_its_output() {
    NotificationHandler<Notification1> handler1 = getMockOfNotificationHandlerForType(Notification1.class);
    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    List<Notification1> notification1s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification1())
      .toList();
    List<Notification2> notification2s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification2())
      .toList();
    NotificationService onlyHandler1 = new NotificationService(dbClient, new NotificationHandler[]{handler1});
    NotificationService onlyHandler2 = new NotificationService(dbClient, new NotificationHandler[]{handler2});
    NotificationService bothHandlers = new NotificationService(dbClient, new NotificationHandler[]{handler1, handler2});

    int expected = notification1s.size() - 1;
    when(handler1.deliver(notification1s)).thenReturn(expected);
    assertThat(onlyHandler1.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1, times(1)).deliver(notification1s);
    verify(handler2, times(0)).deliver(anyCollection());

    expected = notification2s.size() - 2;
    when(handler2.deliver(notification2s)).thenReturn(expected);
    assertThat(onlyHandler2.deliverEmails(notification2s)).isEqualTo(expected);
    verify(handler2, times(1)).deliver(notification2s);
    verify(handler1, times(1)).deliver(anyCollection());

    expected = notification1s.size() - 1;
    when(handler1.deliver(notification1s)).thenReturn(expected);
    assertThat(bothHandlers.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1, times(2)).deliver(notification1s);
    verify(handler2, times(1)).deliver(anyCollection());

    expected = notification2s.size() - 2;
    when(handler2.deliver(notification2s)).thenReturn(expected);
    assertThat(bothHandlers.deliverEmails(notification2s)).isEqualTo(expected);
    verify(handler2, times(2)).deliver(notification2s);
    verify(handler1, times(2)).deliver(anyCollection());
  }

  @Test
  public void deliver_calls_deliver_method_on_each_handler_for_notification_class_and_returns_sum_of_their_outputs() {
    NotificationHandler<Notification1> handler1A = getMockOfNotificationHandlerForType(Notification1.class);
    NotificationHandler<Notification1> handler1B = getMockOfNotificationHandlerForType(Notification1.class);
    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);

    List<Notification1> notification1s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification1())
      .toList();
    List<Notification2> notification2s = IntStream.range(0, 10)
      .mapToObj(i -> new Notification2())
      .toList();
    NotificationService onlyHandler1A = new NotificationService(dbClient, new NotificationHandler[]{handler1A});
    NotificationService onlyHandler1B = new NotificationService(dbClient, new NotificationHandler[]{handler1B});
    NotificationService bothHandlers = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B});
    NotificationService allHandlers = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B, handler2});

    int expected = notification1s.size() - 1;
    when(onlyHandler1A.deliverEmails(notification1s)).thenReturn(expected);
    assertThat(onlyHandler1A.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1A, times(1)).deliver(notification1s);
    verify(handler1B, times(0)).deliver(anyCollection());
    verify(handler2, times(0)).deliver(anyCollection());

    expected = notification1s.size() - 1;
    when(handler1B.deliver(notification1s)).thenReturn(expected);
    assertThat(onlyHandler1B.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1B, times(1)).deliver(notification1s);
    verify(handler1A, times(1)).deliver(anyCollection());
    verify(handler2, times(0)).deliver(anyCollection());

    expected = notification1s.size() - 1;
    int expected2 = notification1s.size() - 2;
    when(handler1A.deliver(notification1s)).thenReturn(expected);
    when(handler1B.deliver(notification1s)).thenReturn(expected2);
    assertThat(bothHandlers.deliverEmails(notification1s)).isEqualTo(expected + expected2);
    verify(handler1A, times(2)).deliver(notification1s);
    verify(handler1B, times(2)).deliver(notification1s);
    verify(handler2, times(0)).deliver(anyCollection());

    expected = notification2s.size() - 2;
    when(handler2.deliver(notification2s)).thenReturn(expected);
    assertThat(allHandlers.deliverEmails(notification2s)).isEqualTo(expected);
    verify(handler2, times(1)).deliver(notification2s);
    verify(handler1A, times(2)).deliver(anyCollection());
    verify(handler1B, times(2)).deliver(anyCollection());
  }

  @Test
  public void hasProjectSubscribersForType_returns_false_if_there_are_no_handler() {
    String projectUuid = secure().nextAlphabetic(7);
    NotificationService underTest = new NotificationService(dbClient);

    assertThat(underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification1.class))).isFalse();
    assertThat(underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification2.class))).isFalse();
  }

  @Test
  public void hasProjectSubscribersForType_checks_property_for_each_dispatcher_key_supporting_Notification_type() {
    String dispatcherKey1A = secure().nextAlphabetic(5);
    String dispatcherKey1B = secure().nextAlphabetic(6);
    String projectUuid = secure().nextAlphabetic(7);

    NotificationHandler<Notification1> handler1A = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1A.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1A)));

    NotificationHandler<Notification1> handler1B = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1B.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1B)));

    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    when(handler2.getMetadata()).thenReturn(Optional.empty());

    boolean expected = true;
    when(propertiesDao.hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B)))
      .thenReturn(expected);
    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B, handler2});

    boolean flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification1.class));

    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B));
    verifyNoMoreInteractions(propertiesDao);
    assertThat(flag).isEqualTo(expected);

    flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification1.class, Notification2.class));

    verify(propertiesDao, times(2)).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B));
    verifyNoMoreInteractions(propertiesDao);
    assertThat(flag).isEqualTo(expected);
  }

  @Test
  public void hasProjectSubscribersForType_checks_property_for_each_dispatcher_key_supporting_Notification_types() {
    String dispatcherKey1A = secure().nextAlphabetic(5);
    String dispatcherKey1B = secure().nextAlphabetic(6);
    String dispatcherKey2 = secure().nextAlphabetic(7);
    String projectUuid = secure().nextAlphabetic(8);
    NotificationHandler<Notification1> handler1A = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1A.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1A)));

    NotificationHandler<Notification1> handler1B = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1B.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1B)));

    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    when(handler2.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey2)));

    boolean expected1 = false;
    when(propertiesDao.hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B, dispatcherKey2)))
      .thenReturn(expected1);
    boolean expected2 = true;
    when(propertiesDao.hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey2)))
      .thenReturn(expected2);
    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B, handler2});

    boolean flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification1.class, Notification2.class));

    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B, dispatcherKey2));
    verifyNoMoreInteractions(propertiesDao);
    assertThat(flag).isEqualTo(expected1);

    flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification2.class));

    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey1A, dispatcherKey1B, dispatcherKey2));
    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of(dispatcherKey2));
    verifyNoMoreInteractions(propertiesDao);
    assertThat(flag).isEqualTo(expected2);
  }

  @Test
  public void hasProjectSubscribersForType_returns_false_if_set_is_empty() {
    String dispatcherKey1A = secure().nextAlphabetic(5);
    String dispatcherKey1B = secure().nextAlphabetic(6);
    String projectUuid = secure().nextAlphabetic(7);

    NotificationHandler<Notification1> handler1A = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1A.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1A)));

    NotificationHandler<Notification1> handler1B = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1B.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1B)));

    NotificationHandler<Notification2> handler2 = getMockOfNotificationHandlerForType(Notification2.class);
    when(handler2.getMetadata()).thenReturn(Optional.empty());

    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B, handler2});

    boolean flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of());

    assertThat(flag).isFalse();
    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of());
    verifyNoMoreInteractions(propertiesDao);
  }

  @Test
  public void hasProjectSubscribersForType_returns_false_for_type_which_have_no_handler() {
    String dispatcherKey1A = secure().nextAlphabetic(5);
    String dispatcherKey1B = secure().nextAlphabetic(6);
    String projectUuid = secure().nextAlphabetic(7);

    NotificationHandler<Notification1> handler1A = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1A.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1A)));

    NotificationHandler<Notification1> handler1B = getMockOfNotificationHandlerForType(Notification1.class);
    when(handler1B.getMetadata()).thenReturn(Optional.of(NotificationDispatcherMetadata.create(dispatcherKey1B)));

    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[]{handler1A, handler1B});

    boolean flag = underTest.hasProjectSubscribersForTypes(projectUuid, ImmutableSet.of(Notification2.class));

    assertThat(flag).isFalse();
    verify(propertiesDao).hasProjectNotificationSubscribersForDispatchers(projectUuid, ImmutableSet.of());
    verifyNoMoreInteractions(propertiesDao);
  }

  private static final class Notification1 extends Notification {

    public Notification1() {
      super("1");
    }
  }

  private <T extends Notification> NotificationHandler<T> getMockOfNotificationHandlerForType(Class<T> notificationClass) {
    NotificationHandler mock = mock(NotificationHandler.class);
    when(mock.getNotificationClass()).thenReturn(notificationClass);
    return mock;
  }

  private static final class Notification2 extends Notification {

    public Notification2() {
      super("2");
    }
  }
}
