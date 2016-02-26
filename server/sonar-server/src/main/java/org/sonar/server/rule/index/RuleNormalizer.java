/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule.index;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.Indexable;

/**
 * Only used by RuleMapping and RuleMapper, should be removed
 */
@Deprecated
public class RuleNormalizer {

  public static final String UPDATED_AT_FIELD = "updatedAt";

  public static final class RuleParamField extends Indexable {

    public static final IndexField NAME = add(IndexField.Type.STRING, "name");
    public static final IndexField TYPE = add(IndexField.Type.STRING, "type");
    public static final IndexField DESCRIPTION = addSearchable(IndexField.Type.TEXT, "description");
    public static final IndexField DEFAULT_VALUE = add(IndexField.Type.STRING, "defaultValue");

    public static final Set<IndexField> ALL_FIELDS = ImmutableSet.of(NAME, TYPE, DESCRIPTION, DEFAULT_VALUE);
  }

  public static final class RuleField extends Indexable {

    /**
     * @deprecated because key should be used instead of id. This field is kept for compatibility with
     * SQALE console.
     */
    @Deprecated
    public static final IndexField ID = addSortable(IndexField.Type.DOUBLE, "id");

    public static final IndexField KEY = addSortable(IndexField.Type.STRING, "key");
    public static final IndexField _KEY = add(IndexField.Type.STRING, "_key");
    public static final IndexField REPOSITORY = add(IndexField.Type.STRING, "repo");
    public static final IndexField RULE_KEY = add(IndexField.Type.STRING, "ruleKey");

    public static final IndexField NAME = addSortableAndSearchable(IndexField.Type.STRING, "name");
    public static final IndexField CREATED_AT = addSortable(IndexField.Type.DATE, "createdAt");
    public static final IndexField UPDATED_AT = addSortable(IndexField.Type.DATE, UPDATED_AT_FIELD);
    public static final IndexField HTML_DESCRIPTION = addSearchable(IndexField.Type.TEXT, "htmlDesc");
    public static final IndexField MARKDOWN_DESCRIPTION = add(IndexField.Type.TEXT, "mdDesc");
    public static final IndexField SEVERITY = add(IndexField.Type.STRING, "severity");
    public static final IndexField STATUS = add(IndexField.Type.STRING, "status");
    public static final IndexField FIX_DESCRIPTION = add(IndexField.Type.STRING, "effortToFix");
    public static final IndexField LANGUAGE = add(IndexField.Type.STRING, "lang");
    public static final IndexField TAGS = add(IndexField.Type.STRING, "tags");
    public static final IndexField SYSTEM_TAGS = add(IndexField.Type.STRING, "sysTags");
    public static final IndexField INTERNAL_KEY = add(IndexField.Type.STRING, "internalKey");
    public static final IndexField IS_TEMPLATE = add(IndexField.Type.BOOLEAN, "isTemplate");
    public static final IndexField TEMPLATE_KEY = add(IndexField.Type.STRING, "templateKey");

    public static final IndexField DEFAULT_CHARACTERISTIC = add(IndexField.Type.STRING, "_debtChar");
    public static final IndexField DEFAULT_SUB_CHARACTERISTIC = add(IndexField.Type.STRING, "_debtSubChar");
    public static final IndexField DEFAULT_DEBT_FUNCTION_TYPE = add(IndexField.Type.STRING, "_debtRemFnType");
    public static final IndexField DEFAULT_DEBT_FUNCTION_COEFFICIENT = add(IndexField.Type.STRING, "_debtRemFnCoefficient");
    public static final IndexField DEFAULT_DEBT_FUNCTION_OFFSET = add(IndexField.Type.STRING, "_debtRemFnOffset");

    public static final IndexField CHARACTERISTIC = add(IndexField.Type.STRING, "debtChar");
    public static final IndexField SUB_CHARACTERISTIC = add(IndexField.Type.STRING, "debtSubChar");
    public static final IndexField DEBT_FUNCTION_TYPE = add(IndexField.Type.STRING, "debtRemFnType");
    public static final IndexField DEBT_FUNCTION_COEFFICIENT = add(IndexField.Type.STRING, "debtRemFnCoefficient");
    public static final IndexField DEBT_FUNCTION_OFFSET = add(IndexField.Type.STRING, "debtRemFnOffset");

    public static final IndexField CHARACTERISTIC_OVERLOADED = add(IndexField.Type.BOOLEAN, "debtCharOverloaded");
    public static final IndexField SUB_CHARACTERISTIC_OVERLOADED = add(IndexField.Type.BOOLEAN, "debtSubCharOverloaded");
    public static final IndexField DEBT_FUNCTION_TYPE_OVERLOADED = add(IndexField.Type.BOOLEAN, "debtRemFnTypeOverloaded");

    public static final IndexField NOTE = add(IndexField.Type.TEXT, "markdownNote");
    public static final IndexField NOTE_LOGIN = add(IndexField.Type.STRING, "noteLogin");
    public static final IndexField NOTE_CREATED_AT = add(IndexField.Type.DATE, "noteCreatedAt");
    public static final IndexField NOTE_UPDATED_AT = add(IndexField.Type.DATE, "noteUpdatedAt");
    public static final IndexField ALL_TAGS = addSearchable(IndexField.Type.STRING, "allTags");
    public static final IndexField PARAMS = addEmbedded("params", RuleParamField.ALL_FIELDS);

    public static final Set<IndexField> ALL_FIELDS = ImmutableSet.of(ID, KEY, _KEY, REPOSITORY, RULE_KEY, NAME, CREATED_AT,
      UPDATED_AT, HTML_DESCRIPTION, MARKDOWN_DESCRIPTION, SEVERITY, STATUS, FIX_DESCRIPTION,
      LANGUAGE, TAGS, SYSTEM_TAGS, INTERNAL_KEY, IS_TEMPLATE, TEMPLATE_KEY, DEFAULT_DEBT_FUNCTION_TYPE,
      DEFAULT_DEBT_FUNCTION_COEFFICIENT, DEFAULT_DEBT_FUNCTION_OFFSET, DEBT_FUNCTION_TYPE, DEBT_FUNCTION_COEFFICIENT,
      DEBT_FUNCTION_OFFSET, DEFAULT_CHARACTERISTIC, DEFAULT_SUB_CHARACTERISTIC, CHARACTERISTIC, SUB_CHARACTERISTIC,
      DEBT_FUNCTION_TYPE_OVERLOADED, CHARACTERISTIC_OVERLOADED, SUB_CHARACTERISTIC_OVERLOADED,
      NOTE, NOTE_LOGIN, NOTE_CREATED_AT, NOTE_UPDATED_AT, ALL_TAGS, PARAMS);

    /**
     * Warning - O(n) complexity
     */
    public static IndexField of(String fieldName) {
      for (IndexField field : ALL_FIELDS) {
        if (field.field().equals(fieldName)) {
          return field;
        }
      }
      throw new IllegalStateException("Could not find an IndexField for '" + fieldName + "'");
    }
  }

}
