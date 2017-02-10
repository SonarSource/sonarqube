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
import { translate } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({
  prepareSearch () {
    let url = window.baseUrl + '/api/issues/tags?ps=10';
    const tags = this.options.app.state.get('query').tags;
    if (tags != null) {
      url += '&tags=' + tags;
    }
    return this.$('.js-custom-value').select2({
      placeholder: translate('search_verb'),
      minimumInputLength: 0,
      allowClear: false,
      formatNoMatches () {
        return translate('select2.noMatches');
      },
      formatSearching () {
        return translate('select2.searching');
      },
      width: '100%',
      ajax: {
        url,
        quietMillis: 300,
        data (term) {
          return { q: term, ps: 10 };
        },
        results (data) {
          const results = data.tags.map(tag => {
            return { id: tag, text: tag };
          });
          return { more: false, results };
        }
      }
    });
  },

  getValuesWithLabels () {
    const values = this.model.getValues();
    values.forEach(v => {
      v.label = v.val;
      v.extra = '';
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
