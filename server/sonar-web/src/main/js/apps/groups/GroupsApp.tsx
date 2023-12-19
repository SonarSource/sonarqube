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
import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import GitHubSynchronisationWarning from '../../app/components/GitHubSynchronisationWarning';
import GitLabSynchronisationWarning from '../../app/components/GitLabSynchronisationWarning';
import ListFooter from '../../components/controls/ListFooter';
import { ManagedFilter } from '../../components/controls/ManagedFilter';
import SearchBox from '../../components/controls/SearchBox';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { translate } from '../../helpers/l10n';
import { useGroupsQueries } from '../../queries/groups';
import { useIdentityProviderQuery } from '../../queries/identity-provider/common';
import { Provider } from '../../types/types';
import Header from './components/Header';
import List from './components/List';
import './groups.css';

export default function GroupsApp() {
  const [search, setSearch] = useState<string>('');
  const [managed, setManaged] = useState<boolean | undefined>();
  const { data: manageProvider } = useIdentityProviderQuery();

  const { data, isLoading, fetchNextPage } = useGroupsQueries({
    q: search,
    managed,
  });

  const groups = data?.pages.flatMap((page) => page.groups) ?? [];

  return (
    <>
      <Suggestions suggestions="user_groups" />
      <Helmet defer={false} title={translate('user_groups.page')} />
      <main className="page page-limited" id="groups-page">
        <Header manageProvider={manageProvider?.provider} />
        {manageProvider?.provider === Provider.Github && <GitHubSynchronisationWarning short />}
        {manageProvider?.provider === Provider.Gitlab && <GitLabSynchronisationWarning short />}

        <div className="display-flex-justify-start big-spacer-bottom big-spacer-top">
          <ManagedFilter
            manageProvider={manageProvider?.provider}
            loading={isLoading}
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

        <List groups={groups} manageProvider={manageProvider?.provider} />

        <div id="groups-list-footer">
          <ListFooter
            count={groups.length}
            loading={isLoading}
            loadMore={fetchNextPage}
            ready={!isLoading}
            total={data?.pages[0].page.total}
          />
        </div>
      </main>
    </>
  );
}
