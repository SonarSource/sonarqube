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
import { renameBranch } from '../../../api/branches';
import { Branch } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import Modal from '../../../components/controls/Modal';

interface Props {
  branch: Branch;
  component: string;
  onClose: () => void;
  onRename: () => void;
}

interface State {
  loading: boolean;
  name?: string;
}

export default class RenameBranchModal extends React.PureComponent<Props, State> {
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
    if (!this.state.name) {
      return;
    }
    this.setState({ loading: true });
    renameBranch(this.props.component, this.state.name).then(
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
          this.props.onRename();
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

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  render() {
    const { branch } = this.props;
    const header = translate('branches.rename');
    const submitDisabled =
      this.state.loading || !this.state.name || this.state.name === branch.name;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="rename-branch-name">
                {translate('new_name')}
                <em className="mandatory">*</em>
              </label>
              <input
                autoFocus={true}
                id="rename-branch-name"
                maxLength={100}
                name="name"
                onChange={this.handleNameChange}
                required={true}
                size={50}
                type="text"
                value={this.state.name !== undefined ? this.state.name : branch.name}
              />
            </div>
          </div>
          <footer className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <button disabled={submitDisabled} type="submit">
              {translate('rename')}
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
