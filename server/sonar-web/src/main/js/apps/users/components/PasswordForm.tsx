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
import Modal from '../../../components/controls/Modal';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { User } from '../../../app/types';
import { parseError } from '../../../helpers/request';
import { changePassword } from '../../../api/users';
import { translate } from '../../../helpers/l10n';

interface Props {
  isCurrentUser: boolean;
  user: User;
  onClose: () => void;
}

interface State {
  confirmPassword: string;
  error?: string;
  newPassword: string;
  oldPassword: string;
  submitting: boolean;
}

export default class PasswordForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    confirmPassword: '',
    newPassword: '',
    oldPassword: '',
    submitting: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleError = (error: { response: Response }) => {
    if (!this.mounted || error.response.status !== 400) {
      return throwGlobalError(error);
    } else {
      return parseError(error).then(
        errorMsg => this.setState({ error: errorMsg, submitting: false }),
        throwGlobalError
      );
    }
  };

  handleConfirmPasswordChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ confirmPassword: event.currentTarget.value });

  handleNewPasswordChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ newPassword: event.currentTarget.value });

  handleOldPasswordChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ oldPassword: event.currentTarget.value });

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleChangePassword = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (
      this.state.newPassword.length > 0 &&
      this.state.newPassword === this.state.confirmPassword
    ) {
      this.setState({ submitting: true });
      changePassword({
        login: this.props.user.login,
        password: this.state.newPassword,
        previousPassword: this.state.oldPassword
      }).then(() => {
        addGlobalSuccessMessage(translate('my_profile.password.changed'));
        this.props.onClose();
      }, this.handleError);
    }
  };

  render() {
    const { error, submitting, newPassword, confirmPassword } = this.state;

    const header = translate('my_profile.password.title');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="user-password-form" onSubmit={this.handleChangePassword} autoComplete="off">
          <header className="modal-head">
            <h2>{header}</h2>
          </header>
          <div className="modal-body">
            {error && <p className="alert alert-danger">{error}</p>}
            {this.props.isCurrentUser && (
              <div className="modal-field">
                <label htmlFor="old-user-password">
                  {translate('my_profile.password.old')}
                  <em className="mandatory">*</em>
                </label>
                {/* keep this fake field to hack browser autofill */}
                <input name="old-password-fake" type="password" className="hidden" />
                <input
                  id="old-user-password"
                  name="old-password"
                  type="password"
                  maxLength={50}
                  onChange={this.handleOldPasswordChange}
                  required={true}
                  value={this.state.oldPassword}
                />
              </div>
            )}
            <div className="modal-field">
              <label htmlFor="user-password">
                {translate('my_profile.password.new')}
                <em className="mandatory">*</em>
              </label>
              {/* keep this fake field to hack browser autofill */}
              <input name="password-fake" type="password" className="hidden" />
              <input
                id="user-password"
                name="password"
                type="password"
                maxLength={50}
                onChange={this.handleNewPasswordChange}
                required={true}
                value={this.state.newPassword}
              />
            </div>
            <div className="modal-field">
              <label htmlFor="confirm-user-password">
                {translate('my_profile.password.confirm')}
                <em className="mandatory">*</em>
              </label>
              {/* keep this fake field to hack browser autofill */}
              <input name="confirm-password-fake" type="password" className="hidden" />
              <input
                id="confirm-user-password"
                name="confirm-password"
                type="password"
                maxLength={50}
                onChange={this.handleConfirmPasswordChange}
                required={true}
                value={this.state.confirmPassword}
              />
            </div>
          </div>
          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <button
              className="js-confirm"
              disabled={submitting || !newPassword || newPassword !== confirmPassword}
              type="submit">
              {translate('change_verb')}
            </button>
            <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </footer>
        </form>
      </Modal>
    );
  }
}
