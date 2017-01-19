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
import CustomValuesFacet from './custom-values-facet';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({

  getUrl () {
    const q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return window.baseUrl + '/api/components/search_view_components';
    } else {
      return window.baseUrl + '/api/components/search?qualifiers=TRK';
    }
  },

  prepareAjaxSearch () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data (term, page) {
        return { q: term, p: page };
      },
      results: r => ({
        more: r.paging.total > r.paging.pageIndex * r.paging.pageSize,
        results: r.components.map(component => ({
          id: component.id,
          text: component.name
        }))
      })
    };
  },

  prepareSearch () {
    const q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return this.prepareSearchForViews();
    } else {
      return CustomValuesFacet.prototype.prepareSearch.apply(this, arguments);
    }
  },

  prepareSearchForViews () {
    const componentId = this.options.app.state.get('contextComponentUuid');
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
        data (term, page) {
          return {
            componentId,
            q: term,
            p: page,
            ps: 25
          };
        },
        results (data) {
          return {
            more: data.p * data.ps < data.total,
            results: data.components.map(c => {
              return { id: c.uuid, text: c.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels () {
    const values = this.model.getValues();
    const projects = this.options.app.facets.components;
    values.forEach(v => {
      const uuid = v.val;
      let label = '';
      if (uuid) {
        const project = projects.find(p => p.uuid === uuid);
        if (project != null) {
          label = project.longName;
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
