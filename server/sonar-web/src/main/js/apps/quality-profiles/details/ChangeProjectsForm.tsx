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
import * as React from 'react';
import * as escapeHtml from 'escape-html';
import Modal from '../../../components/controls/Modal';
import SelectList from '../../../components/SelectList';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  organization: string | null;
  profile: Profile;
}

export default class ChangeProjectsForm extends React.PureComponent<Props> {
  container?: HTMLElement | null;

  handleCloseClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  renderSelectList = () => {
    if (this.container) {
      const { key } = this.props.profile;

      const searchUrl =
        (window as any).baseUrl + '/api/qualityprofiles/projects?key=' + encodeURIComponent(key);

      new SelectList({
        searchUrl,
        el: this.container,
        width: '100%',
        readOnly: false,
        focusSearch: false,
        dangerouslyUnescapedHtmlFormat: (item: { name: string }) => escapeHtml(item.name),
        selectUrl: (window as any).baseUrl + '/api/qualityprofiles/add_project',
        deselectUrl: (window as any).baseUrl + '/api/qualityprofiles/remove_project',
        extra: { profileKey: key },
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
    }
  };

  render() {
    const header = translate('projects');

    return (
      <Modal
        contentLabel={header}
        onAfterOpen={this.renderSelectList}
        onRequestClose={this.props.onClose}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          <div id="profile-projects" ref={node => (this.container = node)} />
        </div>

        <div className="modal-foot">
          <a href="#" onClick={this.handleCloseClick}>
            {translate('close')}
          </a>
        </div>
      </Modal>
    );
  }
}
