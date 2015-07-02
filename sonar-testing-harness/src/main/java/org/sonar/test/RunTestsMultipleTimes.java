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
package org.sonar.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class RunTestsMultipleTimes implements TestRule {
  final int threads;
  final int times;

  public RunTestsMultipleTimes(int times, int threads) {
    this.times = times;
    this.threads = threads;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < times; i++) {
          final int index = i;

          results.add(executor.submit(new Runnable() {
            @Override
            public void run() {
              try {
                System.out.println(index);
                base.evaluate();
              } catch (RuntimeException e) {
                throw e;
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            }
          }));
        }

        for (Future<?> result : results) {
          try {
            result.get();
          } catch (ExecutionException e) {
            throw e.getCause();
          }
        }

        executor.shutdown();
      }
    };
  }
}