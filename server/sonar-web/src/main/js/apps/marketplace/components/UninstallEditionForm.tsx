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
import { Edition, EditionStatus, uninstallEdition } from '../../../api/marketplace';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  edition?: Edition;
  editionStatus: EditionStatus;
  onClose: () => void;
  updateEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  loading: boolean;
}

export default class UninstallEditionForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleConfirmClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    uninstallEdition()
      .then(() => {
        this.props.updateEditionStatus({
          ...this.props.editionStatus,
          currentEditionKey: undefined,
          installationStatus: 'UNINSTALL_IN_PROGRESS'
        });
        this.props.onClose();
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  render() {
    const { edition } = this.props;
    const { loading } = this.state;
    const currentEdition = edition ? edition.name : translate('marketplace.commercial_edition');
    const header = translateWithParameters('marketplace.downgrade_to_community_edition');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body">
          <p>{translateWithParameters('marketplace.uninstall_x_confirmation', currentEdition)}</p>
        </div>

        <footer className="modal-foot">
          {loading && <i className="spinner spacer-right" />}
          <button disabled={loading} onClick={this.handleConfirmClick}>
            {translate('marketplace.downgrade')}
          </button>
          <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
