/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import {
  ButtonPrimary,
  ButtonSecondary,
  FormField,
  InputField,
  Link,
  Spinner,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';

interface Props {
  collapsed?: boolean;
  onSubmit: (login: string, password: string) => Promise<void>;
}

interface State {
  collapsed: boolean;
  loading: boolean;
  login: string;
  password: string;
}

const LOGIN_INPUT_ID = 'login-input';
const PASSWORD_INPUT_ID = 'password-input';

export default class LoginForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      collapsed: Boolean(props.collapsed),
      loading: false,
      login: '',
      password: '',
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

  handleMoreOptionsClick = () => {
    this.setState({ collapsed: false });
  };

  handleLoginChange = (event: React.ChangeEvent<HTMLInputElement>) =>
    this.setState({ login: event.currentTarget.value });

  handlePwdChange = (event: React.ChangeEvent<HTMLInputElement>) =>
    this.setState({ password: event.currentTarget.value });

  render() {
    if (this.state.collapsed) {
      return (
        <ButtonSecondary
          className="sw-w-full sw-justify-center"
          aria-expanded={false}
          onClick={this.handleMoreOptionsClick}
        >
          {translate('login.more_options')}
        </ButtonSecondary>
      );
    }
    return (
      <form className="sw-w-full" onSubmit={this.handleSubmit}>
        <FormField label={translate('login')} htmlFor={LOGIN_INPUT_ID} required>
          <InputField
            autoFocus
            id={LOGIN_INPUT_ID}
            maxLength={255}
            name="login"
            onChange={this.handleLoginChange}
            required
            type="text"
            value={this.state.login}
            size="full"
          />
        </FormField>

        <FormField label={translate('password')} htmlFor={PASSWORD_INPUT_ID} required>
          <InputField
            id={PASSWORD_INPUT_ID}
            name="password"
            onChange={this.handlePwdChange}
            required
            type="password"
            value={this.state.password}
            size="full"
          />
        </FormField>

        <div>
          <div className="sw-overflow-hidden sw-flex sw-items-center sw-justify-end sw-gap-3">
            <Spinner loading={this.state.loading} />
            <Link to="/">{translate('go_back')}</Link>
            <ButtonPrimary disabled={this.state.loading} type="submit">
              {translate('sessions.log_in')}
            </ButtonPrimary>
          </div>
        </div>
      </form>
    );
  }
}
