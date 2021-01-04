/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { changePassword } from '../../api/users';

interface Props {
  user: T.LoggedInUser;
  onPasswordChange?: () => void;
}

interface State {
  errors?: string[];
  success: boolean;
}

export default class ResetPassword extends React.Component<Props, State> {
  oldPassword: HTMLInputElement | null = null;
  password: HTMLInputElement | null = null;
  passwordConfirmation: HTMLInputElement | null = null;
  state: State = {
    success: false
  };

  handleSuccessfulChange = () => {
    if (!this.oldPassword || !this.password || !this.passwordConfirmation) {
      return;
    }
    this.oldPassword.value = '';
    this.password.value = '';
    this.passwordConfirmation.value = '';
    this.setState({ success: true, errors: undefined });
    if (this.props.onPasswordChange) {
      this.props.onPasswordChange();
    }
  };

  setErrors = (errors: string[]) => {
    this.setState({ success: false, errors });
  };

  handleChangePassword = (event: React.FormEvent) => {
    event.preventDefault();
    if (!this.oldPassword || !this.password || !this.passwordConfirmation) {
      return;
    }
    const { user } = this.props;
    const previousPassword = this.oldPassword.value;
    const password = this.password.value;
    const passwordConfirmation = this.passwordConfirmation.value;

    if (password !== passwordConfirmation) {
      this.password.focus();
      this.setErrors([translate('user.password_doesnt_match_confirmation')]);
    } else {
      changePassword({ login: user.login, password, previousPassword }).then(
        this.handleSuccessfulChange,
        () => {
          // error already reported.
        }
      );
    }
  };

  render() {
    const { success, errors } = this.state;

    return (
      <section className="boxed-group">
        <h2 className="spacer-bottom">{translate('my_profile.password.title')}</h2>

        <form className="boxed-group-inner" onSubmit={this.handleChangePassword}>
          {success && <Alert variant="success">{translate('my_profile.password.changed')}</Alert>}

          {errors &&
            errors.map((e, i) => (
              /* eslint-disable-next-line react/no-array-index-key */
              <Alert key={i} variant="error">
                {e}
              </Alert>
            ))}

          <div className="form-field">
            <label htmlFor="old_password">
              {translate('my_profile.password.old')}
              <em className="mandatory">*</em>
            </label>
            <input
              autoComplete="off"
              id="old_password"
              name="old_password"
              ref={elem => (this.oldPassword = elem)}
              required={true}
              type="password"
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">
              {translate('my_profile.password.new')}
              <em className="mandatory">*</em>
            </label>
            <input
              autoComplete="off"
              id="password"
              name="password"
              ref={elem => (this.password = elem)}
              required={true}
              type="password"
            />
          </div>
          <div className="form-field">
            <label htmlFor="password_confirmation">
              {translate('my_profile.password.confirm')}
              <em className="mandatory">*</em>
            </label>
            <input
              autoComplete="off"
              id="password_confirmation"
              name="password_confirmation"
              ref={elem => (this.passwordConfirmation = elem)}
              required={true}
              type="password"
            />
          </div>
          <div className="form-field">
            <SubmitButton id="change-password">{translate('update_verb')}</SubmitButton>
          </div>
        </form>
      </section>
    );
  }
}
