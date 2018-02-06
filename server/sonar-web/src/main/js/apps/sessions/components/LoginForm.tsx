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
import { Link } from 'react-router';
import OAuthProviders from './OAuthProviders';
import GlobalMessagesContainer from '../../../app/components/GlobalMessagesContainer';
import { IdentityProvider } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import './LoginForm.css';

interface Props {
  onSonarCloud: boolean;
  identityProviders: IdentityProvider[];
  onSubmit: (login: string, password: string) => void;
  returnTo: string;
}

interface State {
  collapsed: boolean;
  login: string;
  password: string;
}

export default class LoginForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: props.identityProviders.length > 0,
      login: '',
      password: ''
    };
  }

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.props.onSubmit(this.state.login, this.state.password);
  };

  handleMoreOptionsClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ collapsed: false });
  };

  handleLoginChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ login: event.currentTarget.value });

  handlePwdChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ password: event.currentTarget.value });

  render() {
    const loginTitle = this.props.onSonarCloud
      ? translate('login.login_to_sonarcloud')
      : translate('login.login_to_sonarqube');

    return (
      <div className="login-page" id="login_form">
        <h1 className="login-title text-center">{loginTitle}</h1>

        {this.props.identityProviders.length > 0 && (
          <OAuthProviders
            identityProviders={this.props.identityProviders}
            returnTo={this.props.returnTo}
          />
        )}

        {this.state.collapsed ? (
          <div className="text-center">
            <a
              className="small text-muted js-more-options"
              href="#"
              onClick={this.handleMoreOptionsClick}>
              {translate('login.more_options')}
            </a>
          </div>
        ) : (
          <form className="login-form" onSubmit={this.handleSubmit}>
            <GlobalMessagesContainer />

            <div className="big-spacer-bottom">
              <label htmlFor="login" className="login-label">
                {translate('login')}
              </label>
              <input
                type="text"
                id="login"
                name="login"
                className="login-input"
                maxLength={255}
                required={true}
                autoFocus={true}
                placeholder={translate('login')}
                value={this.state.login}
                onChange={this.handleLoginChange}
              />
            </div>

            <div className="big-spacer-bottom">
              <label htmlFor="password" className="login-label">
                {translate('password')}
              </label>
              <input
                type="password"
                id="password"
                name="password"
                className="login-input"
                required={true}
                placeholder={translate('password')}
                value={this.state.password}
                onChange={this.handlePwdChange}
              />
            </div>

            <div>
              <div className="text-right overflow-hidden">
                <button name="commit" type="submit">
                  {translate('sessions.log_in')}
                </button>
                <Link className="spacer-left" to="/">
                  {translate('cancel')}
                </Link>
              </div>
            </div>
          </form>
        )}
      </div>
    );
  }
}
