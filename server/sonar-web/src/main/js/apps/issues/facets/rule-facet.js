/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import CustomValuesFacet from './custom-values-facet';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({
  prepareSearch() {
    let url = window.baseUrl + '/api/rules/search?f=name,langName';
    const languages = this.options.app.state.get('query').languages;
    if (languages != null) {
      url += '&languages=' + languages;
    }
    return this.$('.js-custom-value').select2({
      placeholder: translate('search_verb'),
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches() {
        return translate('select2.noMatches');
      },
      formatSearching() {
        return translate('select2.searching');
      },
      formatInputTooShort() {
        return translateWithParameters('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        url,
        quietMillis: 300,
        data(term, page) {
          return { q: term, p: page };
        },
        results(data) {
          const results = data.rules.map(rule => {
            const lang = rule.langName || translate('manual');
            return {
              id: rule.key,
              text: '(' + lang + ') ' + rule.name
            };
          });
          return {
            more: data.p * data.ps < data.total,
            results
          };
        }
      }
    });
  },

  getValuesWithLabels() {
    const values = this.model.getValues();
    const rules = this.options.app.facets.rules;
    values.forEach(v => {
      const key = v.val;
      let label = '';
      let extra = '';
      if (key) {
        const rule = rules.find(r => r.key === key);
        if (rule != null) {
          label = rule.name;
        }
        if (rule != null) {
          extra = rule.langName;
        }
      }
      v.label = label;
      v.extra = extra;
    });
    return values;
  },

  serializeData() {
    return {
      ...CustomValuesFacet.prototype.serializeData.apply(this, arguments),
      values: this.sortValues(this.getValuesWithLabels())
    };
  }
});
