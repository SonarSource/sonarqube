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
package org.sonar.batch.protocol.input;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileTest {

  @Test
  public void testEqualsAndHashCode() throws ParseException {
    QProfile qProfile1 = new QProfile("squid-java", "Java", "java", new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1984"));
    QProfile qProfile2 = new QProfile("squid-java", "Java 2", "java", new SimpleDateFormat("dd/MM/yyyy").parse("14/03/1985"));

    assertThat(qProfile1.equals(qProfile1)).isTrue();
    assertThat(qProfile1.equals("foo")).isFalse();
    assertThat(qProfile1.equals(qProfile2)).isTrue();

    assertThat(qProfile1.hashCode()).isEqualTo(148572637);
  }
}
