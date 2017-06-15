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
import React from 'react';
import Modal from 'react-modal';
import escapeHtml from 'escape-html';
import type { Profile } from '../propTypes';
import { translate } from '../../../helpers/l10n';

type Props = {
  onClose: () => void,
  organization: ?string,
  profile: Profile
};

export default class ChangeProjectsForm extends React.PureComponent {
  container: HTMLElement;
  props: Props;

  componentDidMount() {
    this.renderSelectList();
  }

  handleCloseClick = (event: Event) => {
    event.preventDefault();
    this.props.onClose();
  };

  renderSelectList() {
    const { key } = this.props.profile;

    const searchUrl =
      window.baseUrl + '/api/qualityprofiles/projects?key=' + encodeURIComponent(key);

    new window.SelectList({
      searchUrl,
      el: this.container,
      width: '100%',
      readOnly: false,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat: item => escapeHtml(item.name),
      selectUrl: window.baseUrl + '/api/qualityprofiles/add_project',
      deselectUrl: window.baseUrl + '/api/qualityprofiles/remove_project',
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

  render() {
    const header = translate('projects');

    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body">
          <div id="profile-projects" ref={node => (this.container = node)} />
        </div>

        <div className="modal-foot">
          <a href="#" onClick={this.handleCloseClick}>{translate('close')}</a>
        </div>

      </Modal>
    );
  }
}
