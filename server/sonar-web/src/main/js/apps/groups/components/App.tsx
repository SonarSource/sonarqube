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
import { Helmet } from 'react-helmet';
import Header from './Header';
import List from './List';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { searchUsersGroups, deleteGroup, updateGroup, createGroup } from '../../../api/user_groups';
import { translate } from '../../../helpers/l10n';

interface Props {
  organization?: Pick<T.Organization, 'key'>;
}

interface State {
  groups?: T.Group[];
  loading: boolean;
  paging?: T.Paging;
  query: string;
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, query: '' };

  componentDidMount() {
    this.mounted = true;
    this.fetchGroups();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  get organization() {
    return this.props.organization && this.props.organization.key;
  }

  makeFetchGroupsRequest = (data?: { p?: number; q?: string }) => {
    this.setState({ loading: true });
    return searchUsersGroups({
      organization: this.organization,
      q: this.state.query,
      ...data
    });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchGroups = (data?: { p?: number; q?: string }) => {
    this.makeFetchGroupsRequest(data).then(({ groups, paging }) => {
      if (this.mounted) {
        this.setState({ groups, loading: false, paging });
      }
    }, this.stopLoading);
  };

  fetchMoreGroups = () => {
    const { paging } = this.state;
    if (paging && paging.total > paging.pageIndex * paging.pageSize) {
      this.makeFetchGroupsRequest({ p: paging.pageIndex + 1 }).then(({ groups, paging }) => {
        if (this.mounted) {
          this.setState(({ groups: existingGroups = [] }) => ({
            groups: [...existingGroups, ...groups],
            loading: false,
            paging
          }));
        }
      }, this.stopLoading);
    }
  };

  search = (query: string) => {
    this.fetchGroups({ q: query });
    this.setState({ query });
  };

  refresh = () => {
    this.fetchGroups({ q: this.state.query });
  };

  handleCreate = (data: { description: string; name: string }) => {
    return createGroup({ ...data, organization: this.organization }).then(group => {
      if (this.mounted) {
        this.setState(({ groups = [] }: State) => ({
          groups: [...groups, group]
        }));
      }
    });
  };

  handleDelete = (name: string) => {
    return deleteGroup({ name, organization: this.organization }).then(() => {
      if (this.mounted) {
        this.setState(({ groups = [] }: State) => ({
          groups: groups.filter(group => group.name !== name)
        }));
      }
    });
  };

  handleEdit = (data: { description?: string; id: number; name?: string }) => {
    return updateGroup(data).then(() => {
      if (this.mounted) {
        this.setState(({ groups = [] }: State) => ({
          groups: groups.map(group => (group.id === data.id ? { ...group, ...data } : group))
        }));
      }
    });
  };

  render() {
    const { groups, loading, paging, query } = this.state;

    const showAnyone =
      this.props.organization === undefined && 'anyone'.includes(query.toLowerCase());

    return (
      <>
        <Suggestions suggestions="user_groups" />
        <Helmet title={translate('user_groups.page')} />
        <div className="page page-limited" id="groups-page">
          <Header loading={loading} onCreate={this.handleCreate} />

          <SearchBox
            className="big-spacer-bottom"
            id="groups-search"
            minLength={2}
            onChange={this.search}
            placeholder={translate('search.search_by_name')}
            value={query}
          />

          {groups !== undefined && (
            <List
              groups={groups}
              onDelete={this.handleDelete}
              onEdit={this.handleEdit}
              onEditMembers={this.refresh}
              organization={this.organization}
              showAnyone={showAnyone}
            />
          )}

          {groups !== undefined && paging !== undefined && (
            <div id="groups-list-footer">
              <ListFooter
                count={showAnyone ? groups.length + 1 : groups.length}
                loadMore={this.fetchMoreGroups}
                ready={!loading}
                total={showAnyone ? paging.total + 1 : paging.total}
              />
            </div>
          )}
        </div>
      </>
    );
  }
}
