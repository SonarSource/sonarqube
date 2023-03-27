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
import React, { useCallback, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { getIdentityProviders, searchUsers } from '../../api/users';
import ListFooter from '../../components/controls/ListFooter';
import { ManagedFilter } from '../../components/controls/ManagedFilter';
import SearchBox from '../../components/controls/SearchBox';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { useManageProvider } from '../../components/hooks/useManageProvider';
import DeferredSpinner from '../../components/ui/DeferredSpinner';
import { translate } from '../../helpers/l10n';
import { IdentityProvider, Paging } from '../../types/types';
import { User } from '../../types/users';
import Header from './Header';
import UsersList from './UsersList';

export default function UsersApp() {
  const [identityProviders, setIdentityProviders] = useState<IdentityProvider[]>([]);

  const [loading, setLoading] = useState(true);
  const [paging, setPaging] = useState<Paging>();
  const [users, setUsers] = useState<User[]>([]);

  const [search, setSearch] = useState('');
  const [managed, setManaged] = useState<boolean | undefined>(undefined);

  const manageProvider = useManageProvider();

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const { paging, users } = await searchUsers({ q: search, managed });
      setPaging(paging);
      setUsers(users);
    } finally {
      setLoading(false);
    }
  }, [search, managed]);

  const fetchMoreUsers = useCallback(async () => {
    if (!paging) {
      return;
    }
    setLoading(true);
    try {
      const { paging: nextPage, users: nextUsers } = await searchUsers({
        q: search,
        managed,
        p: paging.pageIndex + 1,
      });
      setPaging(nextPage);
      setUsers([...users, ...nextUsers]);
    } finally {
      setLoading(false);
    }
  }, [search, managed, paging, users]);

  useEffect(() => {
    (async () => {
      const { identityProviders } = await getIdentityProviders();
      setIdentityProviders(identityProviders);
    })();
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [search, managed]);

  return (
    <main className="page page-limited" id="users-page">
      <Suggestions suggestions="users" />
      <Helmet defer={false} title={translate('users.page')} />
      <Header onUpdateUsers={fetchUsers} manageProvider={manageProvider} />
      <div className="display-flex-justify-start big-spacer-bottom big-spacer-top">
        <ManagedFilter
          manageProvider={manageProvider}
          loading={loading}
          managed={managed}
          setManaged={setManaged}
        />
        <SearchBox
          id="users-search"
          minLength={2}
          onChange={(search: string) => setSearch(search)}
          placeholder={translate('search.search_by_login_or_name')}
          value={search}
        />
      </div>
      <DeferredSpinner loading={loading}>
        <UsersList
          identityProviders={identityProviders}
          onUpdateUsers={fetchUsers}
          updateTokensCount={fetchUsers}
          users={users}
          manageProvider={manageProvider}
        />
      </DeferredSpinner>
      {paging !== undefined && (
        <ListFooter
          count={users.length}
          loadMore={fetchMoreUsers}
          ready={!loading}
          total={paging.total}
        />
      )}
    </main>
  );
}
