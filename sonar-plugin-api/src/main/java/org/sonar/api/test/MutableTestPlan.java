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
package org.sonar.api.test;

/**
 * @deprecated since 5.6. Feature will be removed without any alternatives.
 */
@Deprecated
public interface MutableTestPlan extends TestPlan<MutableTestCase> {

  /**
   * Add a {@link TestCase} to this test file.
   * Note that a same physical test (for example in Java a single method annotated with @Test)
   * can be executed several times (parameterized tests, different test suites, ...). As a result it is perfectly valid to register several
   * tests with the same name. Anyway in this situation the coverage per test will be merged for all tests with the same name.
   * @param name
   * @return
   */
  MutableTestCase addTestCase(String name);

}
