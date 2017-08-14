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
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import LoginForm from './LoginForm';
import { doLogin } from '../../../store/rootActions';
import { getAppState } from '../../../store/rootReducer';
import { getIdentityProviders } from '../../../api/users';

class LoginFormContainer extends React.PureComponent {
  /*:: mounted: boolean; */

  static propTypes = {
    location: PropTypes.object.isRequired
  };

  state = {};

  componentDidMount() {
    this.mounted = true;
    getIdentityProviders().then(r => {
      if (this.mounted) {
        this.setState({ identityProviders: r.identityProviders });
      }
    });
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSuccessfulLogin = () => {
    const { location } = this.props;
    const queryReturnTo = location.query['return_to'];
    const returnTo = queryReturnTo ? `${queryReturnTo}${location.hash}` : `${window.baseUrl}/`;
    window.location = returnTo;
  };

  handleSubmit = (login /*: string */, password /*: string */) => {
    this.props.doLogin(login, password).then(this.handleSuccessfulLogin, () => {
      /* do nothing */
    });
  };

  render() {
    if (!this.state.identityProviders) {
      return null;
    }

    return (
      <LoginForm identityProviders={this.state.identityProviders} onSubmit={this.handleSubmit} />
    );
  }
}

const mapStateToProps = state => ({
  appState: getAppState(state)
});

const mapDispatchToProps = { doLogin };

export default connect(mapStateToProps, mapDispatchToProps)(LoginFormContainer);
