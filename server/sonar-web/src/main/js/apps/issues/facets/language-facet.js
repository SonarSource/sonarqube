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
  getUrl () {
    return window.baseUrl + '/api/languages/list';
  },

  prepareSearch () {
    return this.$('.js-custom-value').select2({
      placeholder: translate('search_verb'),
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches () {
        return translate('select2.noMatches');
      },
      formatSearching () {
        return translate('select2.searching');
      },
      formatInputTooShort () {
        return translateWithParameters('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data (term) {
          return { q: term, ps: 0 };
        },
        results (data) {
          return {
            more: false,
            results: data.languages.map(lang => {
              return { id: lang.key, text: lang.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels () {
    const values = this.model.getValues();
    const source = this.options.app.facets.languages;
    values.forEach(v => {
      const key = v.val;
      let label = null;
      if (key) {
        const item = source.find(lang => lang.key === key);
        if (item != null) {
          label = item.name;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData () {
    return {
      ...CustomValuesFacet.prototype.serializeData.apply(this, arguments),
      values: this.sortValues(this.getValuesWithLabels())
    };
  }
});
