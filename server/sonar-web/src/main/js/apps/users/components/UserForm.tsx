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
import { uniq } from 'lodash';
import UserScmAccountInput from './UserScmAccountInput';
import Modal from '../../../components/controls/Modal';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { parseError } from '../../../helpers/request';
import { createUser, updateUser } from '../../../api/users';
import { User } from '../../../app/types';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  user?: User;
  onClose: () => void;
  onUpdateUsers: () => void;
}

interface State {
  email: string;
  error?: string;
  login: string;
  name: string;
  password: string;
  scmAccounts: string[];
  submitting: boolean;
}

export default class UserForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const { user } = props;
    if (user) {
      this.state = {
        email: user.email || '',
        login: user.login,
        name: user.name,
        password: '',
        scmAccounts: user.scmAccounts || [],
        submitting: false
      };
    } else {
      this.state = {
        email: '',
        login: '',
        name: '',
        password: '',
        scmAccounts: [],
        submitting: false
      };
    }
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleError = (error: { response: Response }) => {
    if (!this.mounted || ![400, 500].includes(error.response.status)) {
      return throwGlobalError(error);
    } else {
      return parseError(error).then(
        errorMsg => this.setState({ error: errorMsg, submitting: false }),
        throwGlobalError
      );
    }
  };

  handleEmailChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ email: event.currentTarget.value });

  handleLoginChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ login: event.currentTarget.value });

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ name: event.currentTarget.value });

  handlePasswordChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ password: event.currentTarget.value });

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleCreateUser = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    createUser({
      email: this.state.email || undefined,
      login: this.state.login,
      name: this.state.name,
      password: this.state.password,
      scmAccount: uniq(this.state.scmAccounts)
    }).then(() => {
      this.props.onUpdateUsers();
      this.props.onClose();
    }, this.handleError);
  };

  handleUpdateUser = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    updateUser({
      email: this.state.email,
      login: this.state.login,
      name: this.state.name,
      scmAccount: uniq(this.state.scmAccounts).join(',')
    }).then(() => {
      this.props.onUpdateUsers();
      this.props.onClose();
    }, this.handleError);
  };

  handleAddScmAccount = (evt: React.SyntheticEvent<HTMLButtonElement>) => {
    evt.preventDefault();
    this.setState(({ scmAccounts }) => ({ scmAccounts: scmAccounts.concat('') }));
  };

  handleUpdateScmAccount = (idx: number, scmAccount: string) =>
    this.setState(({ scmAccounts: oldScmAccounts }) => {
      const scmAccounts = oldScmAccounts.slice();
      scmAccounts[idx] = scmAccount;
      return { scmAccounts };
    });

  handleRemoveScmAccount = (idx: number) =>
    this.setState(({ scmAccounts }) => ({
      scmAccounts: scmAccounts.slice(0, idx).concat(scmAccounts.slice(idx + 1))
    }));

  render() {
    const { user } = this.props;
    const { error, submitting } = this.state;

    const header = user ? translate('users.update_user') : translate('users.create_user');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form
          id="user-form"
          onSubmit={this.props.user ? this.handleUpdateUser : this.handleCreateUser}
          autoComplete="off">
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {error && <p className="alert alert-danger">{error}</p>}

            {!user && (
              <div className="modal-field">
                <label htmlFor="create-user-login">
                  {translate('login')}
                  <em className="mandatory">*</em>
                </label>
                {/* keep this fake field to hack browser autofill */}
                <input name="login-fake" type="text" className="hidden" />
                <input
                  id="create-user-login"
                  name="login"
                  type="text"
                  minLength={3}
                  maxLength={255}
                  onChange={this.handleLoginChange}
                  required={true}
                  value={this.state.login}
                />
                <p className="note">{translateWithParameters('users.minimum_x_characters', 3)}</p>
              </div>
            )}
            <div className="modal-field">
              <label htmlFor="create-user-name">
                {translate('name')}
                <em className="mandatory">*</em>
              </label>
              {/* keep this fake field to hack browser autofill */}
              <input name="name-fake" type="text" className="hidden" />
              <input
                id="create-user-name"
                name="name"
                type="text"
                maxLength={200}
                onChange={this.handleNameChange}
                required={true}
                value={this.state.name}
              />
            </div>
            <div className="modal-field">
              <label htmlFor="create-user-email">{translate('users.email')}</label>
              {/* keep this fake field to hack browser autofill */}
              <input name="email-fake" type="email" className="hidden" />
              <input
                id="create-user-email"
                name="email"
                type="email"
                maxLength={100}
                onChange={this.handleEmailChange}
                value={this.state.email}
              />
            </div>
            {!user && (
              <div className="modal-field">
                <label htmlFor="create-user-password">
                  {translate('password')}
                  <em className="mandatory">*</em>
                </label>
                {/* keep this fake field to hack browser autofill */}
                <input name="password-fake" type="password" className="hidden" />
                <input
                  id="create-user-password"
                  name="password"
                  type="password"
                  maxLength={50}
                  onChange={this.handlePasswordChange}
                  required={true}
                  value={this.state.password}
                />
              </div>
            )}
            <div className="modal-field">
              <label>{translate('my_profile.scm_accounts')}</label>
              {this.state.scmAccounts.map((scm, idx) => (
                <UserScmAccountInput
                  idx={idx}
                  key={idx}
                  onChange={this.handleUpdateScmAccount}
                  onRemove={this.handleRemoveScmAccount}
                  scmAccount={scm}
                />
              ))}
              <div className="spacer-bottom">
                <button onClick={this.handleAddScmAccount}>{translate('add_verb')}</button>
              </div>
              <p className="note">{translate('user.login_or_email_used_as_scm_account')}</p>
            </div>
          </div>

          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <button className="js-confirm" disabled={submitting} type="submit">
              {user ? translate('update_verb') : translate('create')}
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
