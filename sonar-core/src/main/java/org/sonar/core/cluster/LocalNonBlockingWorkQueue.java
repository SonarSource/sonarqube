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

import org.jfree.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalNonBlockingWorkQueue implements WorkQueue{

  private ConcurrentLinkedQueue<IndexAction> actions;

  public LocalNonBlockingWorkQueue(){
    this.actions = new ConcurrentLinkedQueue<IndexAction>();
  }

  @Override
  public Integer enqueue(IndexAction... indexActions){
    for(IndexAction action:indexActions){
      actions.offer(action);
    }
    return 0;
  }

  @Override
  public Object dequeue(){
    Object out = actions.poll();
    while(out == null){
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Log.error("Oops");
      }
      out = actions.poll();
    }
    return out;
  }

  @Override
  public Status getStatus(Integer workId) {
    // TODO Auto-generated method stub
    return null;
  }

}
