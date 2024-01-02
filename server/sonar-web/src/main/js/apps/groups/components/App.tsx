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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { createGroup, deleteGroup, searchUsersGroups, updateGroup } from '../../../api/user_groups';
import ListFooter from '../../../components/controls/ListFooter';
import SearchBox from '../../../components/controls/SearchBox';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';
import { Group, Paging } from '../../../types/types';
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
}

export default class App extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { loading: true, query: '' };

  componentDidMount() {
    this.mounted = true;
    this.fetchGroups();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  makeFetchGroupsRequest = (data?: { p?: number; q?: string }) => {
    this.setState({ loading: true });
    return searchUsersGroups({
      q: this.state.query,
      ...data,
    });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchGroups = async (data?: { p?: number; q?: string }) => {
    try {
      const { groups, paging } = await this.makeFetchGroupsRequest(data);
      if (this.mounted) {
        this.setState({ groups, loading: false, paging });
      }
    } catch {
      this.stopLoading();
    }
  };

  fetchMoreGroups = async () => {
    const { paging: currentPaging } = this.state;
    if (currentPaging && currentPaging.total > currentPaging.pageIndex * currentPaging.pageSize) {
      try {
        const { groups, paging } = await this.makeFetchGroupsRequest({
          p: currentPaging.pageIndex + 1,
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

  search = (query: string) => {
    this.fetchGroups({ q: query });
    this.setState({ query });
  };

  refresh = async () => {
    const { paging, query } = this.state;

    await this.fetchGroups({ q: query });

    // reload all pages in order
    if (paging && paging.pageIndex > 1) {
      for (let p = 1; p < paging.pageIndex; p++) {
        // eslint-disable-next-line no-await-in-loop
        await this.fetchMoreGroups(); // This is a intentional promise chain
      }
    }
  };

  closeDeleteForm = () => {
    this.setState({ groupToBeDeleted: undefined });
  };

  closeEditForm = () => {
    this.setState({ editedGroup: undefined });
  };

  openDeleteForm = (group: Group) => {
    this.setState({ groupToBeDeleted: group });
  };

  openEditForm = (group: Group) => {
    this.setState({ editedGroup: group });
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
      description,
      id: editedGroup.id,
      // pass `name` only if it has changed, otherwise the WS fails
      ...omitNil({ name: name !== editedGroup.name ? name : undefined }),
    };

    await updateGroup(data);

    if (this.mounted) {
      this.setState(({ groups = [] }: State) => ({
        editedGroup: undefined,
        groups: groups.map((group) =>
          group.name === editedGroup.name ? { ...group, ...data } : group
        ),
      }));
    }
  };

  render() {
    const { editedGroup, groupToBeDeleted, groups, loading, paging, query } = this.state;

    const showAnyone = 'anyone'.includes(query.toLowerCase());

    return (
      <>
        <Suggestions suggestions="user_groups" />
        <Helmet defer={false} title={translate('user_groups.page')} />
        <div className="page page-limited" id="groups-page">
          <Header onCreate={this.handleCreate} />

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
              onDelete={this.openDeleteForm}
              onEdit={this.openEditForm}
              onEditMembers={this.refresh}
              showAnyone={showAnyone}
            />
          )}

          {groups !== undefined && paging !== undefined && (
            <div id="groups-list-footer">
              <ListFooter
                count={showAnyone ? groups.length + 1 : groups.length}
                loading={loading}
                loadMore={this.fetchMoreGroups}
                ready={!loading}
                total={showAnyone ? paging.total + 1 : paging.total}
              />
            </div>
          )}

          {groupToBeDeleted && (
            <DeleteForm
              group={groupToBeDeleted}
              onClose={this.closeDeleteForm}
              onSubmit={this.handleDelete}
            />
          )}

          {editedGroup && (
            <Form
              confirmButtonText={translate('update_verb')}
              group={editedGroup}
              header={translate('groups.update_group')}
              onClose={this.closeEditForm}
              onSubmit={this.handleEdit}
            />
          )}
        </div>
      </>
    );
  }
}
