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
package org.sonar.server.notification;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.Notification;
import org.sonar.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class NotificationServiceTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final DbClient dbClient = mock(DbClient.class);

  @Test
  public void deliverEmails_fails_with_IAE_if_type_of_collection_is_Notification() {
    NotificationHandler handler = mock(NotificationHandler1.class);
    List<Notification> notifications = IntStream.range(0, 1 + new Random().nextInt(20))
      .mapToObj(i -> new Notification("i"))
      .collect(Collectors.toList());
    NotificationService underTest = new NotificationService(dbClient, new NotificationHandler[] {handler});

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Type of notification objects must be a subtype of Notification");

    underTest.deliverEmails(notifications);
  }

  @Test
  public void deliverEmails_collection_has_no_effect_if_no_handler_nor_dispatcher() {
    List<Notification> notifications = IntStream.range(0, 1 + new Random().nextInt(20))
      .mapToObj(i -> mock(Notification.class))
      .collect(Collectors.toList());
    NotificationService underTest = new NotificationService(dbClient);

    assertThat(underTest.deliverEmails(notifications)).isZero();
    verifyZeroInteractions(dbClient);
  }

  @Test
  public void deliverEmails_collection_has_no_effect_if_no_handler() {
    NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
    List<Notification> notifications = IntStream.range(0, new Random().nextInt(20))
      .mapToObj(i -> mock(Notification.class))
      .collect(Collectors.toList());
    NotificationService underTest = new NotificationService(dbClient, new NotificationDispatcher[] {dispatcher});

    assertThat(underTest.deliverEmails(notifications)).isZero();
    verifyZeroInteractions(dispatcher);
    verifyZeroInteractions(dbClient);
  }

  @Test
  public void deliverEmails_collection_returns_0_if_collection_is_empty() {
    NotificationHandler1 handler1 = mock(NotificationHandler1.class);
    NotificationHandler2 handler2 = mock(NotificationHandler2.class);
    NotificationService underTest = new NotificationService(dbClient,
      new NotificationHandler[] {handler1, handler2});

    assertThat(underTest.deliverEmails(Collections.emptyList())).isZero();
    verifyZeroInteractions(handler1, handler2);
  }

  @Test
  public void deliverEmails_collection_returns_0_if_no_handler_for_the_notification_class() {
    NotificationHandler1 handler1 = mock(NotificationHandler1.class);
    NotificationHandler2 handler2 = mock(NotificationHandler2.class);
    List<Notification1> notification1s = IntStream.range(0, 1 + new Random().nextInt(20))
      .mapToObj(i -> new Notification1())
      .collect(Collectors.toList());
    List<Notification2> notification2s = IntStream.range(0, 1 + new Random().nextInt(20))
      .mapToObj(i -> new Notification2())
      .collect(Collectors.toList());
    NotificationService noHandler = new NotificationService(dbClient);
    NotificationService onlyHandler1 = new NotificationService(dbClient, new NotificationHandler[] {handler1});
    NotificationService onlyHandler2 = new NotificationService(dbClient, new NotificationHandler[] {handler2});

    assertThat(noHandler.deliverEmails(notification1s)).isZero();
    assertThat(noHandler.deliverEmails(notification2s)).isZero();
    assertThat(onlyHandler1.deliverEmails(notification2s)).isZero();
    assertThat(onlyHandler2.deliverEmails(notification1s)).isZero();
    verify(handler1, times(0)).deliver(anyCollection());
    verify(handler2, times(0)).deliver(anyCollection());
  }

  @Test
  public void deliverEmails_collection_calls_deliver_method_of_handler_for_notification_class_and_returns_its_output() {
    Random random = new Random();
    NotificationHandler1 handler1 = mock(NotificationHandler1.class);
    NotificationHandler2 handler2 = mock(NotificationHandler2.class);
    List<Notification1> notification1s = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(i -> new Notification1())
      .collect(Collectors.toList());
    List<Notification2> notification2s = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(i -> new Notification2())
      .collect(Collectors.toList());
    NotificationService onlyHandler1 = new NotificationService(dbClient, new NotificationHandler[] {handler1});
    NotificationService onlyHandler2 = new NotificationService(dbClient, new NotificationHandler[] {handler2});
    NotificationService bothHandlers = new NotificationService(dbClient, new NotificationHandler[] {handler1, handler2});

    int expected = randomDeliveredCount(notification1s);
    when(handler1.deliver(notification1s)).thenReturn(expected);
    assertThat(onlyHandler1.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1).deliver(notification1s);
    verify(handler2, times(0)).deliver(anyCollection());

    reset(handler1, handler2);
    expected = randomDeliveredCount(notification2s);
    when(handler2.deliver(notification2s)).thenReturn(expected);
    assertThat(onlyHandler2.deliverEmails(notification2s)).isEqualTo(expected);
    verify(handler2).deliver(notification2s);
    verify(handler1, times(0)).deliver(anyCollection());

    reset(handler1, handler2);
    expected = randomDeliveredCount(notification1s);
    when(handler1.deliver(notification1s)).thenReturn(expected);
    assertThat(bothHandlers.deliverEmails(notification1s)).isEqualTo(expected);
    verify(handler1).deliver(notification1s);
    verify(handler2, times(0)).deliver(anyCollection());

    reset(handler1, handler2);
    expected = randomDeliveredCount(notification2s);
    when(handler2.deliver(notification2s)).thenReturn(expected);
    assertThat(bothHandlers.deliverEmails(notification2s)).isEqualTo(expected);
    verify(handler2).deliver(notification2s);
    verify(handler1, times(0)).deliver(anyCollection());
  }

  private static final class Notification1 extends Notification {

    public Notification1() {
      super("1");
    }
  }

  private static abstract class NotificationHandler1 implements NotificationHandler<Notification1> {

    // final to prevent mock to override implementation
    @Override
    public final Class<Notification1> getNotificationClass() {
      return Notification1.class;
    }

  }

  private static final class Notification2 extends Notification {

    public Notification2() {
      super("2");
    }
  }

  private static abstract class NotificationHandler2 implements NotificationHandler<Notification2> {

    // final to prevent mock to override implementation
    @Override
    public final Class<Notification2> getNotificationClass() {
      return Notification2.class;
    }
  }

  private static <T extends Notification> int randomDeliveredCount(List<T> notifications) {
    int size = notifications.size();
    if (size == 1) {
      return size;
    }
    return 1 + new Random().nextInt(size - 1);
  }
}
