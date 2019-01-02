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
package org.sonar.server.qualityprofile;

import java.util.Date;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityProfileTest {

  private static final String SOME_QP_KEY = "qpKey";
  private static final String SOME_QP_NAME = "qpName";
  private static final String SOME_LANGUAGE_KEY = "languageKey";
  private static final Date SOME_DATE = DateUtils.parseDateTimeQuietly("2010-05-18T15:50:45+0100");
  private static final QualityProfile QUALITY_PROFILE = new QualityProfile(SOME_QP_KEY, SOME_QP_NAME, SOME_LANGUAGE_KEY, SOME_DATE);

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_qkKey_arg_is_null() {
    new QualityProfile(null, SOME_QP_NAME, SOME_LANGUAGE_KEY, SOME_DATE);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_qpName_arg_is_null() {
    new QualityProfile(SOME_QP_KEY, null, SOME_LANGUAGE_KEY, SOME_DATE);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_languageKey_arg_is_null() {
    new QualityProfile(SOME_QP_KEY, SOME_QP_NAME, null, SOME_DATE);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_rulesUpdatedAt_arg_is_null() {
    new QualityProfile(SOME_QP_KEY, SOME_QP_NAME, SOME_LANGUAGE_KEY, null);
  }

  @Test
  public void verify_properties() {
    assertThat(QUALITY_PROFILE.getQpKey()).isEqualTo(SOME_QP_KEY);
    assertThat(QUALITY_PROFILE.getQpName()).isEqualTo(SOME_QP_NAME);
    assertThat(QUALITY_PROFILE.getLanguageKey()).isEqualTo(SOME_LANGUAGE_KEY);
    assertThat(QUALITY_PROFILE.getRulesUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void verify_getRulesUpdatedAt_keeps_object_immutable() {
    assertThat(QUALITY_PROFILE.getRulesUpdatedAt()).isNotSameAs(SOME_DATE);
  }

  @Test
  public void verify_equals() {
    assertThat(QUALITY_PROFILE).isEqualTo(new QualityProfile(SOME_QP_KEY, SOME_QP_NAME, SOME_LANGUAGE_KEY, SOME_DATE));
    assertThat(QUALITY_PROFILE).isEqualTo(QUALITY_PROFILE);
    assertThat(QUALITY_PROFILE).isNotEqualTo(null);
  }

  @Test
  public void verify_toString() {
    assertThat(QUALITY_PROFILE.toString()).isEqualTo("QualityProfile{key=qpKey, name=qpName, language=languageKey, rulesUpdatedAt=1274194245000}");
  }
}
