/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { renameBranch } from '../../../api/branches';

interface Props {
  branch: T.MainBranch;
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

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  render() {
    const { branch } = this.props;
    const header = translate('branches.rename');
    const submitDisabled =
      this.state.loading || !this.state.name || this.state.name === branch.name;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
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
            <SubmitButton disabled={submitDisabled}>{translate('rename')}</SubmitButton>
            <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
