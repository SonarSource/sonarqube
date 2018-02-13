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
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { Location } from 'history';
import Header from './Header';
import Search from './Search';
import UsersList from './UsersList';
import { parseQuery, Query, serializeQuery } from './utils';
import ListFooter from '../../components/controls/ListFooter';
import { getIdentityProviders, searchUsers } from '../../api/users';
import { Paging, IdentityProvider, User } from '../../app/types';
import { translate } from '../../helpers/l10n';

interface Props {
  currentUser: { isLoggedIn: boolean; login?: string };
  location: Location;
  organizationsEnabled: boolean;
}

interface State {
  identityProviders: IdentityProvider[];
  loading: boolean;
  paging?: Paging;
  users: User[];
}

export default class UsersApp extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  state: State = { identityProviders: [], loading: true, users: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchIdentityProviders();
    this.fetchUsers();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.location.query.search !== this.props.location.query.search) {
      this.fetchUsers(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  finishLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchIdentityProviders = () =>
    getIdentityProviders().then(
      ({ identityProviders }) => {
        if (this.mounted) {
          this.setState({ identityProviders });
        }
      },
      () => {}
    );

  fetchUsers = ({ location } = this.props) => {
    this.setState({ loading: true });
    searchUsers({ q: parseQuery(location.query).search }).then(({ paging, users }) => {
      if (this.mounted) {
        this.setState({ loading: false, paging, users });
      }
    }, this.finishLoading);
  };

  fetchMoreUsers = () => {
    const { paging } = this.state;
    if (paging) {
      this.setState({ loading: true });
      searchUsers({
        p: paging.pageIndex + 1,
        q: parseQuery(this.props.location.query).search
      }).then(({ paging, users }) => {
        if (this.mounted) {
          this.setState(state => ({ loading: false, users: [...state.users, ...users], paging }));
        }
      }, this.finishLoading);
    }
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.context.router.push({ ...this.props.location, query });
  };

  updateTokensCount = (login: string, tokensCount: number) => {
    this.setState(state => ({
      users: state.users.map(user => (user.login === login ? { ...user, tokensCount } : user))
    }));
  };

  render() {
    const query = parseQuery(this.props.location.query);
    const { loading, paging, users } = this.state;
    return (
      <div id="users-page" className="page page-limited">
        <Helmet title={translate('users.page')} />
        <Header loading={loading} onUpdateUsers={this.fetchUsers} />
        <Search query={query} updateQuery={this.updateQuery} />
        <UsersList
          currentUser={this.props.currentUser}
          identityProviders={this.state.identityProviders}
          onUpdateUsers={this.fetchUsers}
          organizationsEnabled={this.props.organizationsEnabled}
          updateTokensCount={this.updateTokensCount}
          users={users}
        />
        {paging !== undefined && (
          <ListFooter
            count={users.length}
            total={paging.total}
            ready={!loading}
            loadMore={this.fetchMoreUsers}
          />
        )}
      </div>
    );
  }
}
