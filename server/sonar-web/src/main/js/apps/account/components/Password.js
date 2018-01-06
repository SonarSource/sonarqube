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
import React, { Component } from 'react';
import { changePassword } from '../../../api/users';
import { translate } from '../../../helpers/l10n';

export default class Password extends Component {
  state = {
    success: false,
    errors: null
  };

  handleSuccessfulChange = () => {
    this.oldPassword.value = '';
    this.password.value = '';
    this.passwordConfirmation.value = '';
    this.setState({ success: true, errors: null });
  };

  handleFailedChange = e => {
    e.response.json().then(r => {
      this.oldPassword.focus();
      this.setErrors(r.errors.map(e => e.msg));
    });
  };

  setErrors = errors => {
    this.setState({
      success: false,
      errors
    });
  };

  handleChangePassword = e => {
    e.preventDefault();

    const { user } = this.props;
    const previousPassword = this.oldPassword.value;
    const password = this.password.value;
    const passwordConfirmation = this.passwordConfirmation.value;

    if (password !== passwordConfirmation) {
      this.password.focus();
      this.setErrors([translate('user.password_doesnt_match_confirmation')]);
    } else {
      changePassword({ login: user.login, password, previousPassword })
        .then(this.handleSuccessfulChange)
        .catch(this.handleFailedChange);
    }
  };

  render() {
    const { success, errors } = this.state;

    return (
      <section className="boxed-group">
        <h2 className="spacer-bottom">{translate('my_profile.password.title')}</h2>

        <form className="boxed-group-inner" onSubmit={this.handleChangePassword}>
          {success && (
            <div className="alert alert-success">{translate('my_profile.password.changed')}</div>
          )}

          {errors &&
            errors.map((e, i) => (
              <div key={i} className="alert alert-danger">
                {e}
              </div>
            ))}

          <div className="modal-field">
            <label htmlFor="old_password">
              {translate('my_profile.password.old')}
              <em className="mandatory">*</em>
            </label>
            <input
              ref={elem => (this.oldPassword = elem)}
              autoComplete="off"
              id="old_password"
              name="old_password"
              required={true}
              type="password"
            />
          </div>
          <div className="modal-field">
            <label htmlFor="password">
              {translate('my_profile.password.new')}
              <em className="mandatory">*</em>
            </label>
            <input
              ref={elem => (this.password = elem)}
              autoComplete="off"
              id="password"
              name="password"
              required={true}
              type="password"
            />
          </div>
          <div className="modal-field">
            <label htmlFor="password_confirmation">
              {translate('my_profile.password.confirm')}
              <em className="mandatory">*</em>
            </label>
            <input
              ref={elem => (this.passwordConfirmation = elem)}
              autoComplete="off"
              id="password_confirmation"
              name="password_confirmation"
              required={true}
              type="password"
            />
          </div>
          <div className="modal-field">
            <button id="change-password" type="submit">
              {translate('my_profile.password.submit')}
            </button>
          </div>
        </form>
      </section>
    );
  }
}
