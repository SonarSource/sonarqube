/*
 * SonarQube :: Web
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
import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({

  getUrl: function () {
    var q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return baseUrl + '/api/components/search_view_components';
    } else {
      return baseUrl + '/api/resources/search?f=s2&q=TRK&display_uuid=true';
    }
  },

  prepareSearch: function () {
    var q = this.options.app.state.get('contextComponentQualifier');
    if (q === 'VW' || q === 'SVW') {
      return this.prepareSearchForViews();
    } else {
      return CustomValuesFacet.prototype.prepareSearch.apply(this, arguments);
    }
  },

  prepareSearchForViews: function () {
    var componentId = this.options.app.state.get('contextComponentUuid');
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches: function () {
        return translate('select2.noMatches');
      },
      formatSearching: function () {
        return translate('select2.searching');
      },
      formatInputTooShort: function () {
        return translateWithParameters('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term, page) {
          return { q: term, componentId: componentId, p: page, ps: 25 };
        },
        results: function (data) {
          return {
            more: data.p * data.ps < data.total,
            results: data.components.map(function (c) {
              return { id: c.uuid, text: c.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        projects = this.options.app.facets.components;
    values.forEach(function (v) {
      var uuid = v.val,
          label = '';
      if (uuid) {
        var project = _.findWhere(projects, { uuid: uuid });
        if (project != null) {
          label = project.longName;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});
