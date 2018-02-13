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
import { bulkDeleteProjects } from '../../api/components';
import { translate, translateWithParameters } from '../../helpers/l10n';
import AlertWarnIcon from '../../components/icons-components/AlertWarnIcon';
import Modal from '../../components/controls/Modal';

export interface Props {
  analyzedBefore?: string;
  onClose: () => void;
  onConfirm: () => void;
  organization: string;
  provisioned: boolean;
  qualifier: string;
  query: string;
  selection: string[];
  total: number;
}

interface State {
  loading: boolean;
}

export default class DeleteModal extends React.PureComponent<Props, State> {
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

  handleConfirmClick = () => {
    this.setState({ loading: true });
    const parameters = this.props.selection.length
      ? {
          organization: this.props.organization,
          projects: this.props.selection.join()
        }
      : {
          analyzedBefore: this.props.analyzedBefore,
          onProvisionedOnly: this.props.provisioned || undefined,
          organization: this.props.organization,
          qualifiers: this.props.qualifier,
          q: this.props.query || undefined
        };
    bulkDeleteProjects(parameters).then(
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

  renderWarning = () => (
    <div className="alert alert-warning modal-alert">
      <AlertWarnIcon className="spacer-right" />
      {this.props.selection.length
        ? translateWithParameters(
            'projects_management.delete_selected_warning',
            this.props.selection.length
          )
        : translateWithParameters('projects_management.delete_all_warning', this.props.total)}
    </div>
  );

  render() {
    const header = translate('qualifiers.delete', this.props.qualifier);

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>

        <div className="modal-body">
          {this.renderWarning()}
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
