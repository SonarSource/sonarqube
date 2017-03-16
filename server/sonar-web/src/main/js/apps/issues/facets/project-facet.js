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
import Template from '../templates/facets/issues-projects-facet.hbs';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { areThereCustomOrganizations, getOrganization } from '../../../store/organizations/utils';

export default CustomValuesFacet.extend({
  template: Template,

  getUrl() {
    return window.baseUrl + '/api/components/search';
  },

  prepareSearchForViews() {
    const contextId = this.options.app.state.get('contextComponentUuid');
    return {
      url: window.baseUrl + '/api/components/tree',
      data(term, page) {
        return { q: term, p: page, qualifiers: 'TRK', baseComponentId: contextId };
      }
    };
  },

  prepareAjaxSearch() {
    const options = {
      quietMillis: 300,
      url: this.getUrl(),
      data(term, page) {
        return { q: term, p: page, qualifiers: 'TRK' };
      },
      results: r => ({
        more: r.paging.total > r.paging.pageIndex * r.paging.pageSize,
        results: r.components.map(component => ({
          id: component.id,
          text: component.name
        }))
      })
    };
    const contextQualifier = this.options.app.state.get('contextComponentQualifier');
    if (contextQualifier === 'VW' || contextQualifier === 'SVW') {
      Object.assign(options, this.prepareSearchForViews());
    }
    return options;
  },

  prepareSearch() {
    return this.$('.js-custom-value').select2({
      placeholder: translate('search_verb'),
      minimumInputLength: 3,
      allowClear: false,
      formatNoMatches() {
        return translate('select2.noMatches');
      },
      formatSearching() {
        return translate('select2.searching');
      },
      formatInputTooShort() {
        return translateWithParameters('select2.tooShort', 3);
      },
      width: '100%',
      ajax: this.prepareAjaxSearch()
    });
  },

  getValuesWithLabels() {
    const values = this.model.getValues();
    const projects = this.options.app.facets.components;
    const displayOrganizations = areThereCustomOrganizations();
    values.forEach(v => {
      const uuid = v.val;
      let label = '';
      let organization = null;
      if (uuid) {
        const project = projects.find(p => p.uuid === uuid);
        if (project != null) {
          label = project.longName;
          organization = displayOrganizations && project.organization
            ? getOrganization(project.organization)
            : null;
        }
      }
      v.label = label;
      v.organization = organization;
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
