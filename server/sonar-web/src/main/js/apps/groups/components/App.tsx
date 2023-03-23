/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { omit } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { getSystemInfo } from '../../../api/system';
import { createGroup, deleteGroup, searchUsersGroups, updateGroup } from '../../../api/user_groups';
import ButtonToggle from '../../../components/controls/ButtonToggle';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';
import { Group, Paging, SysInfoCluster } from '../../../types/types';
import '../groups.css';
import DeleteForm from './DeleteForm';
import Form from './Form';
import Header from './Header';
import List from './List';

interface State {
  groups?: Group[];
  editedGroup?: Group;
  groupToBeDeleted?: Group;
  loading: boolean;
  paging?: Paging;
  query: string;
  manageProvider?: string;
  managed: boolean | undefined;
}

export default class App extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    loading: true,
    query: '',
    managed: undefined,
    paging: { pageIndex: 1, pageSize: 100, total: 1000 },
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchGroups();
    this.fetchManageInstance();
  }

  componentDidUpdate(_prevProps: {}, prevState: State) {
    if (prevState.query !== this.state.query || prevState.managed !== this.state.managed) {
      this.fetchGroups();
    }
    if (prevState !== undefined && prevState.paging?.pageIndex !== this.state.paging?.pageIndex) {
      this.fetchMoreGroups();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async fetchManageInstance() {
    const info = (await getSystemInfo()) as SysInfoCluster;
    if (this.mounted) {
      this.setState({
        manageProvider: info.System['External Users and Groups Provisioning'],
      });
    }
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchGroups = async () => {
    const { query: q, managed } = this.state;
    this.setState({ loading: true });
    try {
      const { groups, paging } = await searchUsersGroups({
        q,
        managed,
      });
      if (this.mounted) {
        this.setState({ groups, loading: false, paging });
      }
    } catch {
      this.stopLoading();
    }
  };

  fetchMoreGroups = async () => {
    const { query: q, managed, paging: currentPaging } = this.state;
    if (currentPaging && currentPaging.total > currentPaging.pageIndex * currentPaging.pageSize) {
      try {
        const { groups, paging } = await searchUsersGroups({
          p: currentPaging.pageIndex,
          q,
          managed,
        });
        if (this.mounted) {
          this.setState(({ groups: existingGroups = [] }) => ({
            groups: [...existingGroups, ...groups],
            loading: false,
            paging,
          }));
        }
      } catch {
        this.stopLoading();
      }
    }
  };

  refresh = async () => {
    const { paging } = this.state;

    await this.fetchGroups();

    // reload all pages in order
    if (paging && paging.pageIndex > 1) {
      for (let p = 1; p < paging.pageIndex; p++) {
        // eslint-disable-next-line no-await-in-loop
        await this.fetchMoreGroups(); // This is a intentional promise chain
      }
    }
  };

  handleCreate = async (data: { description: string; name: string }) => {
    await createGroup({ ...data });

    await this.refresh();
  };

  handleDelete = async () => {
    const { groupToBeDeleted } = this.state;

    if (!groupToBeDeleted) {
      return;
    }

    await deleteGroup({ name: groupToBeDeleted.name });

    await this.refresh();

    if (this.mounted) {
      this.setState({ groupToBeDeleted: undefined });
    }
  };

  handleEdit = async ({ name, description }: { name?: string; description: string }) => {
    const { editedGroup } = this.state;

    if (!editedGroup) {
      return;
    }

    const data = {
      currentName: editedGroup.name,
      description,
      // pass `name` only if it has changed, otherwise the WS fails
      ...omitNil({ name: name !== editedGroup.name ? name : undefined }),
    };

    await updateGroup(data);

    if (this.mounted) {
      this.setState(({ groups = [] }: State) => ({
        editedGroup: undefined,
        groups: groups.map((group) =>
          group.name === editedGroup.name
            ? {
                ...group,
                ...omit(data, ['currentName']),
              }
            : group
        ),
      }));
    }
  };

  render() {
    const {
      editedGroup,
      groupToBeDeleted,
      groups,
      loading,
      paging,
      query,
      manageProvider,
      managed,
    } = this.state;

    return (
      <>
        <Suggestions suggestions="user_groups" />
        <Helmet defer={false} title={translate('user_groups.page')} />
        <main className="page page-limited" id="groups-page">
          <Header onCreate={this.handleCreate} manageProvider={manageProvider} />

          <div className="display-flex-justify-start big-spacer-bottom big-spacer-top">
            {manageProvider !== undefined && (
              <div className="big-spacer-right">
                <ButtonToggle
                  value={managed === undefined ? 'all' : managed}
                  disabled={loading}
                  options={[
                    { label: translate('all'), value: 'all' },
                    { label: translate('managed'), value: true },
                    { label: translate('local'), value: false },
                  ]}
                  onCheck={(filterOption) => {
                    if (filterOption === 'all') {
                      this.setState({ managed: undefined });
                    } else {
                      this.setState({ managed: filterOption as boolean });
                    }
                  }}
                />
              </div>
            )}
            <SearchBox
              className="big-spacer-bottom"
              id="groups-search"
              minLength={2}
              onChange={(q) => this.setState({ query: q })}
              placeholder={translate('search.search_by_name')}
              value={query}
            />
          </div>

          {groups !== undefined && (
            <List
              groups={groups}
              onDelete={(groupToBeDeleted) => this.setState({ groupToBeDeleted })}
              onEdit={(editedGroup) => this.setState({ editedGroup })}
              onEditMembers={this.refresh}
              manageProvider={manageProvider}
            />
          )}

          {groups !== undefined && paging !== undefined && (
            <div id="groups-list-footer">
              <ListFooter
                count={groups.length}
                loading={loading}
                loadMore={() => {
                  if (paging.total > paging.pageIndex * paging.pageSize) {
                    this.setState({ paging: { ...paging, pageIndex: paging.pageIndex + 1 } });
                  }
                }}
                ready={!loading}
                total={paging.total}
              />
            </div>
          )}

          {groupToBeDeleted && (
            <DeleteForm
              group={groupToBeDeleted}
              onClose={() => this.setState({ groupToBeDeleted: undefined })}
              onSubmit={this.handleDelete}
            />
          )}

          {editedGroup && (
            <Form
              confirmButtonText={translate('update_verb')}
              group={editedGroup}
              header={translate('groups.update_group')}
              onClose={() => this.setState({ editedGroup: undefined })}
              onSubmit={this.handleEdit}
            />
          )}
        </main>
      </>
    );
  }
}
