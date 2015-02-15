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
package org.sonar.microbenchmark;

import org.apache.commons.lang.StringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class LoggingBenchmark {

  private Logger logger1 = org.slf4j.LoggerFactory.getLogger("microbenchmark1");
  private Logger logger2 = org.slf4j.LoggerFactory.getLogger("microbenchmark2");

  @Benchmark
  public void logback_converters() throws Exception {
    logger1.warn("many arguments {} {} {} {} {} {}", "foo", 7, 4.5, 1234567890L, true, new Date());
  }

  @Benchmark
  public void string_converters() throws Exception {
    logger2.warn(String.format("many arguments %s %d %f %d %b %tc", "foo", 7, 4.5, 1234567890L,  true, new Date()));
  }

  @Benchmark
  public void logback_no_args() throws Exception {
    logger1.warn("no args");
    StringUtils.replaceOnce()
  }


  /**
   * You can this benchmark with maven command-line (see run.sh) or by executing this method
   * in IDE
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(LoggingBenchmark.class.getSimpleName())
      .build();
    new Runner(opt).run();
  }
}
