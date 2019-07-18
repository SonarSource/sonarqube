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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import './LoginForm.css';

interface Props {
  collapsed?: boolean;
  onSubmit: (login: string, password: string) => Promise<void>;
  returnTo: string;
}

interface State {
  collapsed: boolean;
  loading: boolean;
  login: string;
  password: string;
}

export default class LoginForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: Boolean(props.collapsed),
      loading: false,
      login: '',
      password: ''
    };
  }

  stopLoading = () => {
    this.setState({ loading: false });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    this.props
      .onSubmit(this.state.login, this.state.password)
      .then(this.stopLoading, this.stopLoading);
  };

  handleMoreOptionsClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ collapsed: false });
  };

  handleLoginChange = (event: React.ChangeEvent<HTMLInputElement>) =>
    this.setState({ login: event.currentTarget.value });

  handlePwdChange = (event: React.ChangeEvent<HTMLInputElement>) =>
    this.setState({ password: event.currentTarget.value });

  render() {
    if (this.state.collapsed) {
      return (
        <div className="text-center">
          <a
            className="small text-muted js-more-options"
            href="#"
            onClick={this.handleMoreOptionsClick}>
            {translate('login.more_options')}
          </a>
        </div>
      );
    }
    return (
      <form className="login-form" onSubmit={this.handleSubmit}>
        <div className="big-spacer-bottom">
          <label className="login-label" htmlFor="login">
            {translate('login')}
          </label>
          <input
            autoFocus={true}
            className="login-input"
            id="login"
            maxLength={255}
            name="login"
            onChange={this.handleLoginChange}
            placeholder={translate('login')}
            required={true}
            type="text"
            value={this.state.login}
          />
        </div>

        <div className="big-spacer-bottom">
          <label className="login-label" htmlFor="password">
            {translate('password')}
          </label>
          <input
            className="login-input"
            id="password"
            name="password"
            onChange={this.handlePwdChange}
            placeholder={translate('password')}
            required={true}
            type="password"
            value={this.state.password}
          />
        </div>

        <div>
          <div className="text-right overflow-hidden">
            <DeferredSpinner className="spacer-right" loading={this.state.loading} />
            <SubmitButton disabled={this.state.loading}>
              {translate('sessions.log_in')}
            </SubmitButton>
            <a className="spacer-left" href={`${getBaseUrl()}/`}>
              {translate('cancel')}
            </a>
          </div>
        </div>
      </form>
    );
  }
}
