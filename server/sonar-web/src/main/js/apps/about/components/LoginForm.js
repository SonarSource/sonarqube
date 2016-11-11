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
import React from 'react';
import { Link } from 'react-router';
import { translate } from '../../../helpers/l10n';
import { login } from '../../../api/auth';

export default class LoginForm extends React.Component {
  state = {
    login: '',
    password: '',
    error: false
  };

  handleLogin = () => {
    this.setState({ error: false });
    window.location = window.baseUrl + '/projects/favorite';
  };

  handleFailedLogin = () => {
    this.setState({ error: true });
  };

  handleSubmit = e => {
    e.preventDefault();
    login(this.state.login, this.state.password)
        .then(this.handleLogin, this.handleFailedLogin);
  };

  render () {
    return (
        <form onSubmit={this.handleSubmit}>
          <h2 className="about-login-form-header">Log In to SonarQube</h2>

          {this.state.error && (
              <div className="alert alert-danger">
                {translate('session.flash_notice.authentication_failed')}
              </div>
          )}

          <div className="big-spacer-bottom">
            <label htmlFor="login" className="login-label">{translate('login')}</label>
            <input type="text" id="login" name="login" className="login-input" maxLength="255" required
                   placeholder={translate('login')}
                   value={this.state.login}
                   onChange={e => this.setState({ login: e.target.value })}/>
          </div>

          <div className="big-spacer-bottom">
            <label htmlFor="password" className="login-label">{translate('password')}</label>
            <input type="password" id="password" name="password" className="login-input" required
                   placeholder={translate('password')}
                   value={this.state.password}
                   onChange={e => this.setState({ password: e.target.value })}/>
          </div>

          <div className="text-right">
            <button name="commit" type="submit">{translate('sessions.log_in')}</button>
            <Link className="spacer-left" to="/about">Cancel</Link>
          </div>
        </form>
    );
  }
}
