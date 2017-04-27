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
// @flow
import escapeHtml from 'escape-html';
import ModalFormView from '../../../components/common/modal-form';
import Template from '../templates/quality-profiles-change-projects.hbs';
import { translate } from '../../../helpers/l10n';
import '../../../components/SelectList';

export default ModalFormView.extend({
  template: Template,

  onRender() {
    // TODO remove uuid usage

    ModalFormView.prototype.onRender.apply(this, arguments);

    const { key } = this.options.profile;

    const searchUrl =
      window.baseUrl + '/api/qualityprofiles/projects?key=' + encodeURIComponent(key);

    new window.SelectList({
      searchUrl,
      el: this.$('#profile-projects'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat(item) {
        return escapeHtml(item.name);
      },
      selectUrl: window.baseUrl + '/api/qualityprofiles/add_project',
      deselectUrl: window.baseUrl + '/api/qualityprofiles/remove_project',
      extra: {
        profileKey: key
      },
      selectParameter: 'projectUuid',
      selectParameterValue: 'uuid',
      labels: {
        selected: translate('quality_gates.projects.with'),
        deselected: translate('quality_gates.projects.without'),
        all: translate('quality_gates.projects.all'),
        noResults: translate('quality_gates.projects.noResults')
      },
      tooltips: {
        select: translate('quality_profiles.projects.select_hint'),
        deselect: translate('quality_profiles.projects.deselect_hint')
      }
    });
  },

  onDestroy() {
    this.options.loadProjects();
    ModalFormView.prototype.onDestroy.apply(this, arguments);
  }
});
