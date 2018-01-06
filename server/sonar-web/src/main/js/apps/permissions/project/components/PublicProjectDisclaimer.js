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
// @flow
import React from 'react';
import Modal from '../../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

/*::
type Props = {
  component: {
    name: string,
    qualifier: string
  },
  onClose: () => void,
  onConfirm: () => void
};
*/

export default class PublicProjectDisclaimer extends React.PureComponent {
  /*:: props: Props; */

  handleCancelClick = (event /*: Event */) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = (event /*: Event */) => {
    event.preventDefault();
    this.props.onConfirm();
    this.props.onClose();
  };

  render() {
    const { qualifier } = this.props.component;

    return (
      <Modal contentLabel="modal form" onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>
            {translateWithParameters('projects_role.turn_x_to_public', this.props.component.name)}
          </h2>
        </header>

        <div className="modal-body">
          <p>{translate('projects_role.are_you_sure_to_turn_project_to_public', qualifier)}</p>
          <p className="spacer-top">
            {translate('projects_role.are_you_sure_to_turn_project_to_public.2', qualifier)}
          </p>
        </div>

        <footer className="modal-foot">
          <button id="confirm-turn-to-public" onClick={this.handleConfirmClick}>
            {translate('projects_role.turn_project_to_public', qualifier)}
          </button>
          <a href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
