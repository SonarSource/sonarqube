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
package org.sonar.server.rule2.ws;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.markdown.Markdown;
import org.sonar.server.rule2.Rule;
import org.sonar.server.rule2.RuleParam;
import org.sonar.server.rule2.index.RuleNormalizer;
import org.sonar.server.search.ws.BaseMapping;
import org.sonar.server.text.MacroInterpreter;

/**
 * Conversion between RuleDoc and WS JSON response
 */
public class RuleMapping extends BaseMapping {

  public RuleMapping(Languages languages, MacroInterpreter macroInterpreter) {
    super();
    addIndexStringField("repo", RuleNormalizer.RuleField.REPOSITORY.key());
    addIndexStringField("name", RuleNormalizer.RuleField.NAME.key());
    addIndexDatetimeField("createdAt", RuleNormalizer.RuleField.CREATED_AT.key());
    addField("htmlDesc", new HtmlDescField(macroInterpreter));
    addIndexStringField("severity", RuleNormalizer.RuleField.SEVERITY.key());
    addIndexStringField("status", RuleNormalizer.RuleField.STATUS.key());
    addIndexStringField("internalKey", RuleNormalizer.RuleField.INTERNAL_KEY.key());
    addIndexBooleanField("template", RuleNormalizer.RuleField.TEMPLATE.key());
    addIndexArrayField("tags", RuleNormalizer.RuleField.TAGS.key());
    addIndexArrayField("sysTags", RuleNormalizer.RuleField.SYSTEM_TAGS.key());
    addField("debtChar", new IndexStringField("debtChar", RuleNormalizer.RuleField.CHARACTERISTIC.key()));
    addField("debtChar", new IndexStringField("debtSubChar", RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key()));
    addField("debtRemFn", new IndexStringField("debtRemFnType", RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.key()));
    addField("debtRemFn", new IndexStringField("debtRemFnCoeff", RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.key()));
    addField("debtRemFn", new IndexStringField("debtRemFnOffset", RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.key()));
    addIndexStringField("mdNote", RuleNormalizer.RuleField.NOTE.key());
    addField("htmlNote", new HtmlNoteField(macroInterpreter));
    addIndexStringField("noteLogin", RuleNormalizer.RuleField.NOTE_LOGIN.key());
    addIndexStringField("lang", RuleNormalizer.RuleField.LANGUAGE.key());
    addField("langName", new LangNameField(languages));
    addField("params", new ParamsField());
  }

  private static class ParamsField extends IndexField<Rule> {
    ParamsField() {
      super(RuleNormalizer.RuleField.PARAMS.key());
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
      super(RuleNormalizer.RuleField.LANGUAGE.key());
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
      super(RuleNormalizer.RuleField.NOTE.key());
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
        json.prop("htmlDesc", macroInterpreter.interpret(html));
      }
    }
  }
}
