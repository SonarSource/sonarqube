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

import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_TYPE_KEYWORD;

public class KeywordFieldBuilder<U extends FieldAware<U>> extends StringFieldBuilder<U, KeywordFieldBuilder<U>> {

  protected KeywordFieldBuilder(U indexType, String fieldName) {
    super(indexType, fieldName);
  }

  @Override
  protected boolean getFieldData() {
    return false;
  }

  protected String getFieldType() {
    return FIELD_TYPE_KEYWORD;
  }

  /**
   * By default, field is stored on disk in a column-stride fashion, so that it can later be used for sorting,
   * aggregations, or scripting.
   * Disabling this reduces the size of the index and drop the constraint of single term max size of
   * 32766 bytes (which, if there is no tokenizing enabled on the field, equals the size of the whole data).
   */
  public KeywordFieldBuilder disableSortingAndAggregating() {
    this.disabledDocValues = true;
    return this;
  }
}
