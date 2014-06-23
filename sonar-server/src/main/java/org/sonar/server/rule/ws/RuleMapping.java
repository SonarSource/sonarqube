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
package org.sonar.server.rule.ws;

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.markdown.Markdown;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleParam;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.text.MacroInterpreter;

/**
 * Conversion between RuleDoc and WS JSON response
 */
public class RuleMapping extends BaseMapping {

  public RuleMapping(Languages languages, MacroInterpreter macroInterpreter) {
    super();
    addIndexStringField("repo", RuleNormalizer.RuleField.REPOSITORY.field());
    addIndexStringField("name", RuleNormalizer.RuleField.NAME.field());
    addIndexDatetimeField("createdAt", RuleNormalizer.RuleField.CREATED_AT.field());
    addField("htmlDesc", new HtmlDescField(macroInterpreter));
    addIndexStringField("severity", RuleNormalizer.RuleField.SEVERITY.field());
    addIndexStringField("status", RuleNormalizer.RuleField.STATUS.field());
    addIndexStringField("internalKey", RuleNormalizer.RuleField.INTERNAL_KEY.field());
    addIndexBooleanField("isTemplate", RuleNormalizer.RuleField.IS_TEMPLATE.field());
    addIndexStringField("templateKey", RuleNormalizer.RuleField.TEMPLATE_KEY.field());
    addIndexArrayField("tags", RuleNormalizer.RuleField.TAGS.field());
    addIndexArrayField("sysTags", RuleNormalizer.RuleField.SYSTEM_TAGS.field());
    addIndexStringField("defaultDebtChar", RuleNormalizer.RuleField.DEFAULT_CHARACTERISTIC.field());
    addIndexStringField("defaultDebtSubChar", RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field());
    addField("debtChar", new IndexStringField("debtChar", RuleNormalizer.RuleField.CHARACTERISTIC.field()));
    addField("debtChar", new IndexStringField("debtSubChar", RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()));
    addField("debtRemFn", new IndexStringField("debtRemFnType", RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.field()));
    addField("debtRemFn", new IndexStringField("debtRemFnCoeff", RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.field()));
    addField("debtRemFn", new IndexStringField("debtRemFnOffset", RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.field()));
    addIndexStringField("effortToFixDescription", RuleNormalizer.RuleField.FIX_DESCRIPTION.field());
    addIndexStringField("mdNote", RuleNormalizer.RuleField.NOTE.field());
    addField("htmlNote", new HtmlNoteField(macroInterpreter));
    addIndexStringField("noteLogin", RuleNormalizer.RuleField.NOTE_LOGIN.field());
    addIndexStringField("lang", RuleNormalizer.RuleField.LANGUAGE.field());
    addField("langName", new LangNameField(languages));
    addField("params", new ParamsField());
  }

  private static class ParamsField extends IndexField<Rule> {
    ParamsField() {
      super(RuleNormalizer.RuleField.PARAMS.field());
    }

    @Override
    public void write(JsonWriter json, Rule rule) {
      json.name("params").beginArray();
      for (RuleParam param : rule.params()) {
        json
          .beginObject()
          .prop("key", param.key())
          .prop("desc", param.description())
          .prop("defaultValue", param.defaultValue())
          .endObject();
      }
      json.endArray();
    }
  }

  private static class LangNameField extends IndexField<Rule> {
    private final Languages languages;

    private LangNameField(Languages languages) {
      super(RuleNormalizer.RuleField.LANGUAGE.field());
      this.languages = languages;
    }

    @Override
    public void write(JsonWriter json, Rule rule) {
      String langKey = rule.language();
      Language lang = languages.get(langKey);
      json.prop("langName", lang != null ? lang.getName() : null);
    }
  }

  private static class HtmlNoteField extends IndexField<Rule> {
    private final MacroInterpreter macroInterpreter;

    private HtmlNoteField(MacroInterpreter macroInterpreter) {
      super(RuleNormalizer.RuleField.NOTE.field());
      this.macroInterpreter = macroInterpreter;
    }

    @Override
    public void write(JsonWriter json, Rule rule) {
      String markdownNote = rule.markdownNote();
      if (markdownNote != null) {
        json.prop("htmlNote", macroInterpreter.interpret(Markdown.convertToHtml(markdownNote)));
      }
    }
  }

  private static class HtmlDescField implements Field<Rule> {
    private final MacroInterpreter macroInterpreter;

    private HtmlDescField(MacroInterpreter macroInterpreter) {
      this.macroInterpreter = macroInterpreter;
    }

    @Override
    public void write(JsonWriter json, Rule rule) {
      String html = rule.htmlDescription();
      if (html != null) {
        if (rule.isManual() || rule.templateKey() != null) {
          String desc = StringEscapeUtils.escapeHtml(html);
          desc = desc.replaceAll("\\n", "<br/>");
          json.prop("htmlDesc", desc);
        } else {
          json.prop("htmlDesc", macroInterpreter.interpret(html));
        }
      }
    }
  }
}
