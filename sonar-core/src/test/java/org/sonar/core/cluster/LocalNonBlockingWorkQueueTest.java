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
package org.sonar.core.cluster;

import org.sonar.core.cluster.LocalNonBlockingWorkQueue;

import org.junit.Test;

import java.io.Serializable;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;


public class LocalNonBlockingWorkQueueTest {

  private static final String WORKING_INDEX = "working_index";
  private static final String NON_WORKING_INDEX = "non_working_index";

  @Test
  public void test_insert_queue(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    assertThat(queue.dequeInsert(WORKING_INDEX)).isNull();
    assertThat(queue.dequeInsert(NON_WORKING_INDEX)).isNull();

    queue.enqueInsert(WORKING_INDEX, new Integer(0));
    assertThat(queue.dequeInsert(NON_WORKING_INDEX)).isNull();

    Object dequeued = queue.dequeInsert(WORKING_INDEX);
    assertThat(queue.dequeInsert(NON_WORKING_INDEX)).isNull();
    assertThat(queue.dequeInsert(WORKING_INDEX)).isNull();

    assertThat(dequeued).isEqualTo(new Integer(0));
  }

  @Test
  public void test_update_queue(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    assertThat(queue.dequeUpdate(WORKING_INDEX)).isNull();
    assertThat(queue.dequeUpdate(NON_WORKING_INDEX)).isNull();

    queue.enqueUpdate(WORKING_INDEX, new Integer(0));
    assertThat(queue.dequeUpdate(NON_WORKING_INDEX)).isNull();

    Object dequeued = queue.dequeUpdate(WORKING_INDEX);
    assertThat(queue.dequeUpdate(NON_WORKING_INDEX)).isNull();
    assertThat(queue.dequeUpdate(WORKING_INDEX)).isNull();

    assertThat(dequeued).isEqualTo(new Integer(0));
  }

  @Test
  public void test_delete_queue(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    assertThat(queue.dequeDelete(WORKING_INDEX)).isNull();
    assertThat(queue.dequeDelete(NON_WORKING_INDEX)).isNull();

    queue.enqueDelete(WORKING_INDEX, new Integer(0));
    assertThat(queue.dequeDelete(NON_WORKING_INDEX)).isNull();

    Object dequeued = queue.dequeDelete(WORKING_INDEX);
    assertThat(queue.dequeDelete(NON_WORKING_INDEX)).isNull();
    assertThat(queue.dequeDelete(WORKING_INDEX)).isNull();

    assertThat(dequeued).isEqualTo(new Integer(0));
  }

  @Test
  public void test_enque_seralizable_object(){

    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    class NonSerializable implements Serializable{
      private Object var1;
      private Map<String, Object> objs;
    }

    NonSerializable nonSer = new NonSerializable();
    assertThat(queue.enqueInsert(WORKING_INDEX, nonSer)).isNotNull();

    Object dequeued = queue.dequeInsert(WORKING_INDEX);
    assertThat(queue.dequeInsert(NON_WORKING_INDEX)).isNull();

    assertThat(dequeued).isNotNull();
    assertThat(dequeued.getClass()).isEqualTo(NonSerializable.class);
  }

  @Test
  public void test_under_queue_capacity(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    for(int i = 0; i < 10; i++){
      assertThat(queue.enqueDelete(WORKING_INDEX, i)).isNotNull();
    }

    for(int i = 0; i < 10; i++){
      assertThat(queue.dequeDelete(WORKING_INDEX)).isNotNull();
    }
    assertThat(queue.dequeDelete(WORKING_INDEX)).isNull();

  }

  @Test
  public void test_over_queue_capacity(){
    LocalNonBlockingWorkQueue queue = new LocalNonBlockingWorkQueue();

    for(int i = 0; i < 100; i++){
      assertThat(queue.enqueDelete(WORKING_INDEX, i)).isNotNull();
    }

    for(int i = 0; i < 100; i++){
      assertThat(queue.dequeDelete(WORKING_INDEX)).isNotNull();
    }
    assertThat(queue.dequeDelete(WORKING_INDEX)).isNull();

  }

}
