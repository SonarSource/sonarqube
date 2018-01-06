/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Marionette from 'backbone.marionette';
import escapeHtml from 'escape-html';
import SelectList from '../../../components/SelectList';
import { translate } from '../../../helpers/l10n';

export default Marionette.ItemView.extend({
  template: () => {},

  onRender() {
    const { qualityGate, organization } = this.options;

    const extra = {
      gateId: qualityGate.id
    };
    let orgQuery = '';
    if (organization) {
      extra.organization = organization;
      orgQuery = '&organization=' + organization;
    }

    new SelectList({
      el: this.options.container,
      width: '100%',
      readOnly: !this.options.edit,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat(item) {
        return escapeHtml(item.name);
      },
      searchUrl: `${window.baseUrl}/api/qualitygates/search?gateId=${qualityGate.id}${orgQuery}`,
      selectUrl: window.baseUrl + '/api/qualitygates/select',
      deselectUrl: window.baseUrl + '/api/qualitygates/deselect',
      extra,
      selectParameter: 'projectId',
      selectParameterValue: 'id',
      labels: {
        selected: translate('quality_gates.projects.with'),
        deselected: translate('quality_gates.projects.without'),
        all: translate('quality_gates.projects.all'),
        noResults: translate('quality_gates.projects.noResults')
      },
      tooltips: {
        select: translate('quality_gates.projects.select_hint'),
        deselect: translate('quality_gates.projects.deselect_hint')
      }
    });
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      canEdit: this.options.edit
    };
  }
});
