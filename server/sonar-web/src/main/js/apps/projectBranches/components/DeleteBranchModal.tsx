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
import { deleteBranch } from '../../../api/branches';
import { Branch } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  branch: Branch;
  component: string;
  onClose: () => void;
  onDelete: () => void;
}

interface State {
  loading: boolean;
}

export default class DeleteBranchModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    deleteBranch(this.props.component, this.props.branch.name).then(
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
          this.props.onDelete();
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { branch } = this.props;
    const header = translate('branches.delete');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            {translateWithParameters('branches.delete.are_you_sure', branch.name)}
          </div>
          <footer className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <button className="button-red" disabled={this.state.loading} type="submit">
              {translate('delete')}
            </button>
            <a href="#" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </footer>
        </form>
      </Modal>
    );
  }
}
