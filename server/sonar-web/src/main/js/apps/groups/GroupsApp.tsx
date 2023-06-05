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
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { searchUsersGroups } from '../../api/user_groups';
import GitHubSynchronisationWarning from '../../app/components/GitHubSynchronisationWarning';
import ListFooter from '../../components/controls/ListFooter';
import { ManagedFilter } from '../../components/controls/ManagedFilter';
import SearchBox from '../../components/controls/SearchBox';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { Provider, useManageProvider } from '../../components/hooks/useManageProvider';
import { translate } from '../../helpers/l10n';
import { Group, Paging } from '../../types/types';
import Header from './components/Header';
import List from './components/List';
import './groups.css';

export default function GroupsApp() {
  const [loading, setLoading] = useState<boolean>(true);
  const [paging, setPaging] = useState<Paging>();
  const [search, setSearch] = useState<string>('');
  const [groups, setGroups] = useState<Group[]>([]);
  const [managed, setManaged] = useState<boolean | undefined>();
  const manageProvider = useManageProvider();

  const fetchGroups = useCallback(async () => {
    setLoading(true);
    try {
      const { groups, paging } = await searchUsersGroups({
        q: search,
        managed,
      });
      setGroups(groups);
      setPaging(paging);
    } finally {
      setLoading(false);
    }
  }, [search, managed]);

  const fetchMoreGroups = useCallback(async () => {
    if (!paging) {
      return;
    }
    setLoading(true);
    try {
      const { groups: nextGroups, paging: nextPage } = await searchUsersGroups({
        q: search,
        managed,
        p: paging.pageIndex + 1,
      });
      setPaging(nextPage);
      setGroups([...groups, ...nextGroups]);
    } finally {
      setLoading(false);
    }
  }, [groups, search, managed, paging]);

  useEffect(() => {
    fetchGroups();
  }, [search, managed]);

  return (
    <>
      <Suggestions suggestions="user_groups" />
      <Helmet defer={false} title={translate('user_groups.page')} />
      <main className="page page-limited" id="groups-page">
        <Header reload={fetchGroups} manageProvider={manageProvider} />
        {manageProvider === Provider.Github && <GitHubSynchronisationWarning short />}

        <div className="display-flex-justify-start big-spacer-bottom big-spacer-top">
          <ManagedFilter
            manageProvider={manageProvider}
            loading={loading}
            managed={managed}
            setManaged={setManaged}
          />
          <SearchBox
            id="groups-search"
            minLength={2}
            onChange={(q) => setSearch(q)}
            placeholder={translate('search.search_by_name')}
            value={search}
          />
        </div>

        <List groups={groups} reload={fetchGroups} manageProvider={manageProvider} />

        {paging !== undefined && (
          <div id="groups-list-footer">
            <ListFooter
              count={groups.length}
              loading={loading}
              loadMore={fetchMoreGroups}
              ready={!loading}
              total={paging.total}
            />
          </div>
        )}
      </main>
    </>
  );
}
