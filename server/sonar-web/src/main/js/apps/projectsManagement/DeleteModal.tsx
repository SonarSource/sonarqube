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
import * as React from 'react';
import Modal from 'react-modal';
import { deleteComponents } from '../../api/components';
import { translate } from '../../helpers/l10n';

export interface Props {
  onClose: () => void;
  onConfirm: () => void;
  organization: string;
  qualifier: string;
  selection: string[];
}

interface State {
  loading: boolean;
}

export default class DeleteModal extends React.PureComponent<Props, State> {
  mounted: boolean;
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

  handleConfirmClick = () => {
    this.setState({ loading: true });
    deleteComponents(this.props.selection, this.props.organization).then(
      () => {
        if (this.mounted) {
          this.props.onConfirm();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const header = translate('qualifiers.delete', this.props.qualifier);

    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>
            {header}
          </h2>
        </header>

        <div className="modal-body">
          {translate('qualifiers.delete_confirm', this.props.qualifier)}
        </div>

        <footer className="modal-foot">
          {this.state.loading && <i className="spinner spacer-right" />}
          <button
            className="button-red"
            disabled={this.state.loading}
            onClick={this.handleConfirmClick}>
            {translate('delete')}
          </button>
          <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
            {translate('cancel')}
          </a>
        </footer>
      </Modal>
    );
  }
}
