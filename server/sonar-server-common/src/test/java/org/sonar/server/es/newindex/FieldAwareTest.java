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
package org.sonar.server.es.newindex;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

public class FieldAwareTest {

  @Test
  public void indexType_is_a_reserved_field_name_whatever_the_case() {
    Stream<BiConsumer<TestFieldAware, String>> fieldSetters = Stream.of(
      (testFieldAware, fieldName) -> testFieldAware.createBooleanField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.createByteField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.createDateTimeField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.createDoubleField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.createIntegerField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.createLongField(fieldName),
      (testFieldAware, fieldName) -> testFieldAware.keywordFieldBuilder(fieldName).build(),
      (testFieldAware, fieldName) -> testFieldAware.textFieldBuilder(fieldName).build(),
      (testFieldAware, fieldName) -> testFieldAware.nestedFieldBuilder(fieldName).addKeywordField("foo").build()
    );

    fieldSetters.forEach(c -> {
      TestFieldAware underTest = new TestFieldAware();
      // should not fail for other field name
      c.accept(underTest, randomAlphabetic(1 + new Random().nextInt(10)));
      // fails whatever the case
      Stream.of("indexType", "indextype", "InDexType", "INDEXTYPE")
        .forEach(illegalFieldName -> {
          try {
            c.accept(underTest, illegalFieldName);
            fail("should have thrown a IllegalArgumentException");
          } catch (IllegalArgumentException e) {
            assertThat(e).hasMessage("indexType is a reserved field name");
          }
        });
    });
  }

  private static class TestFieldAware extends FieldAware<TestFieldAware> {
    private String fieldName;
    private Object attributes;

    @Override
    TestFieldAware setFieldImpl(String fieldName, Object attributes) {
      this.fieldName = fieldName;
      this.attributes = attributes;
      return this;
    }
  }
}
