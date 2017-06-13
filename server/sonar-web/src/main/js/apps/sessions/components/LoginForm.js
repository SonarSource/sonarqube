/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import GlobalMessagesContainer from '../../../app/components/GlobalMessagesContainer';
import { translate } from '../../../helpers/l10n';

type Props = {
  identityProviders: Array<{
    backgroundColor: string,
    iconPath: string,
    key: string,
    name: string
  }>,
  onSubmit: (string, string) => void
};

type State = {
  collapsed: boolean,
  login: string,
  password: string
};

export default class LoginForm extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: props.identityProviders.length > 0,
      login: '',
      password: ''
    };
  }

  handleSubmit = (event: Event) => {
    event.preventDefault();
    this.props.onSubmit(this.state.login, this.state.password);
  };

  handleMoreOptionsClick = (event: Event) => {
    event.preventDefault();
    this.setState({ collapsed: false });
  };

  render() {
    return (
      <div id="login_form">
        <h1 className="maintenance-title text-center">{translate('login.login_to_sonarqube')}</h1>

        {this.props.identityProviders.length > 0 &&
          <section className="oauth-providers">
            <ul>
              {this.props.identityProviders.map(identityProvider => (
                <li key={identityProvider.key}>
                  <a
                    href={`${window.baseUrl}/sessions/init/${identityProvider.key}`}
                    style={{ backgroundColor: identityProvider.backgroundColor }}
                    title={`Log in with ${identityProvider.name}`}>
                    <img
                      alt={identityProvider.name}
                      width="20"
                      height="20"
                      src={window.baseUrl + identityProvider.iconPath}
                    />
                    <span>Log in with {identityProvider.name}</span>
                  </a>
                </li>
              ))}
            </ul>
          </section>}

        {this.state.collapsed
          ? <div className="text-center">
              <a
                className="small text-muted js-more-options"
                href="#"
                onClick={this.handleMoreOptionsClick}>
                {translate('login.more_options')}
              </a>
            </div>
          : <form onSubmit={this.handleSubmit}>
              <GlobalMessagesContainer />

              <div className="big-spacer-bottom">
                <label htmlFor="login" className="login-label">{translate('login')}</label>
                <input
                  type="text"
                  id="login"
                  name="login"
                  className="login-input"
                  maxLength="255"
                  required={true}
                  autoFocus={true}
                  placeholder={translate('login')}
                  value={this.state.login}
                  onChange={e => this.setState({ login: e.target.value })}
                />
              </div>

              <div className="big-spacer-bottom">
                <label htmlFor="password" className="login-label">{translate('password')}</label>
                <input
                  type="password"
                  id="password"
                  name="password"
                  className="login-input"
                  required={true}
                  placeholder={translate('password')}
                  value={this.state.password}
                  onChange={e => this.setState({ password: e.target.value })}
                />
              </div>

              <div>
                <div className="text-right overflow-hidden">
                  <button name="commit" type="submit">{translate('sessions.log_in')}</button>
                  <a className="spacer-left" href={window.baseUrl + '/'}>{translate('cancel')}</a>
                </div>
              </div>
            </form>}
      </div>
    );
  }
}
