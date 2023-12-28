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
import { InputSearch, LargeCenteredLayout, PageContentFontWrapper } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import GitHubSynchronisationWarning from '../../app/components/GitHubSynchronisationWarning';
import GitLabSynchronisationWarning from '../../app/components/GitLabSynchronisationWarning';
import ListFooter from '../../components/controls/ListFooter';
import { ManagedFilter } from '../../components/controls/ManagedFilter';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { translate } from '../../helpers/l10n';
import { useGroupsQueries } from '../../queries/groups';
import { useIdentityProviderQuery } from '../../queries/identity-provider/common';
import { Provider } from '../../types/types';
import Header from './components/Header';
import List from './components/List';

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
    <LargeCenteredLayout>
      <PageContentFontWrapper className="sw-my-8 sw-body-sm">
        <Suggestions suggestions="user_groups" />
        <Helmet defer={false} title={translate('user_groups.page')} />
        <main>
          <Header manageProvider={manageProvider?.provider} />
          {manageProvider?.provider === Provider.Github && <GitHubSynchronisationWarning short />}
          {manageProvider?.provider === Provider.Gitlab && <GitLabSynchronisationWarning short />}

          <div className="sw-flex sw-my-4">
            <ManagedFilter
              manageProvider={manageProvider?.provider}
              loading={isLoading}
              managed={managed}
              setManaged={setManaged}
              miui
            />
            <InputSearch
              minLength={2}
              size="large"
              onChange={(q) => setSearch(q)}
              placeholder={translate('search.search_by_name')}
              value={search}
            />
          </div>

          <List groups={groups} manageProvider={manageProvider?.provider} />

          <ListFooter
            count={groups.length}
            loading={isLoading}
            loadMore={fetchNextPage}
            ready={!isLoading}
            total={data?.pages[0].page.total}
            useMIUIButtons
          />
        </main>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
