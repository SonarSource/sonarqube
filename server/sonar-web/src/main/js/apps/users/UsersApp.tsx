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

import { subDays, subSeconds } from 'date-fns';
import {
  HelperHintIcon,
  InputSearch,
  InputSelect,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Spinner,
  StyledPageTitle,
} from 'design-system';
import React, { useEffect, useMemo, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { getIdentityProviders } from '../../api/users';
import GitHubSynchronisationWarning from '../../app/components/GitHubSynchronisationWarning';
import GitLabSynchronisationWarning from '../../app/components/GitLabSynchronisationWarning';
import ListFooter from '../../components/controls/ListFooter';
import { ManagedFilter } from '../../components/controls/ManagedFilter';
import { now, toISO8601WithOffsetString } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';
import { LabelValueSelectOption } from '../../helpers/search';
import { useIdentityProviderQuery } from '../../queries/identity-provider/common';
import { useUsersQueries } from '../../queries/users';
import { IdentityProvider, Provider } from '../../types/types';
import { RestUserDetailed } from '../../types/users';
import Header from './Header';
import UsersList from './UsersList';
import { USERS_ACTIVITY_OPTIONS, USER_INACTIVITY_DAYS_THRESHOLD } from './constants';
import { UserActivity } from './types';

export default function UsersApp() {
  const [identityProviders, setIdentityProviders] = useState<IdentityProvider[]>([]);
  const [search, setSearch] = useState('');
  const [usersActivity, setUsersActivity] = useState<UserActivity>(UserActivity.AnyActivity);
  const [managed, setManaged] = useState<boolean | undefined>(undefined);

  const { data: manageProvider } = useIdentityProviderQuery();

  const usersActivityParams = useMemo(() => {
    const nowDate = now();
    const nowDateMinus30Days = subDays(nowDate, USER_INACTIVITY_DAYS_THRESHOLD);
    const nowDateMinus30DaysAnd1Second = subSeconds(nowDateMinus30Days, 1);

    switch (usersActivity) {
      case UserActivity.ActiveSonarLintUser:
        return {
          sonarLintLastConnectionDateFrom: toISO8601WithOffsetString(nowDateMinus30Days),
        };
      case UserActivity.ActiveSonarQubeUser:
        return {
          sonarQubeLastConnectionDateFrom: toISO8601WithOffsetString(nowDateMinus30Days),
          sonarLintLastConnectionDateTo: toISO8601WithOffsetString(nowDateMinus30DaysAnd1Second),
        };
      case UserActivity.InactiveUser:
        return {
          sonarQubeLastConnectionDateTo: toISO8601WithOffsetString(nowDateMinus30DaysAnd1Second),
        };
      default:
        return {};
    }
  }, [usersActivity]);

  const { data, isLoading, fetchNextPage } = useUsersQueries<RestUserDetailed>({
    q: search,
    managed,
    ...usersActivityParams,
  });

  const users = data?.pages.flatMap((page) => page.users) ?? [];

  useEffect(() => {
    (async () => {
      const { identityProviders } = await getIdentityProviders();
      setIdentityProviders(identityProviders);
    })();
  }, []);

  return (
    <LargeCenteredLayout as="main" id="users-page">
      <PageContentFontWrapper className="sw-my-8 sw-body-sm">
        <Helmet defer={false} title={translate('users.page')} />
        <Header manageProvider={manageProvider?.provider} />
        {manageProvider?.provider === Provider.Github && <GitHubSynchronisationWarning short />}
        {manageProvider?.provider === Provider.Gitlab && <GitLabSynchronisationWarning short />}
        <div className="sw-flex sw-my-4">
          <ManagedFilter
            manageProvider={manageProvider?.provider}
            loading={isLoading}
            managed={managed}
            setManaged={(m) => setManaged(m)}
          />
          <InputSearch
            id="users-search"
            minLength={2}
            onChange={(search: string) => setSearch(search)}
            placeholder={translate('search.search_by_login_or_name')}
            value={search}
          />
          <div className="sw-flex sw-items-center sw-ml-4">
            <StyledPageTitle as="label" className="sw-body-sm-highlight sw-mr-2">
              {translate('users.filter.by')}
            </StyledPageTitle>
            <InputSelect
              className="sw-body-sm"
              size="medium"
              id="users-activity-filter"
              isDisabled={isLoading}
              onChange={(userActivity: LabelValueSelectOption<UserActivity>) =>
                setUsersActivity(userActivity.value)
              }
              options={USERS_ACTIVITY_OPTIONS}
              isSearchable={false}
              placeholder={translate('users.activity_filter.placeholder')}
              aria-label={translate('users.activity_filter.label')}
              value={
                USERS_ACTIVITY_OPTIONS.find((option) => option.value === usersActivity) ?? null
              }
            />
            <HelpTooltip
              className="sw-ml-1"
              overlay={
                <>
                  <p>{translate('users.activity_filter.helptext.sonarqube')}</p>
                  <p>{translate('users.activity_filter.helptext.sonarlint')}</p>
                </>
              }
            >
              <HelperHintIcon />
            </HelpTooltip>
          </div>
        </div>
        <Spinner loading={isLoading}>
          <UsersList
            identityProviders={identityProviders}
            users={users}
            manageProvider={manageProvider?.provider}
          />
        </Spinner>

        <ListFooter
          count={users.length}
          loadMore={fetchNextPage}
          ready={!isLoading}
          total={data?.pages[0].page.total}
        />
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
