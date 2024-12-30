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
import { useNavigate } from 'react-router-dom';
import { SubnavigationGroup, SubnavigationItem } from '~design-system';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { WebApi } from '../../../types/types';
import { Query, actionsFilter, isDomainPathActive, serializeQuery } from '../utils';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';

interface Props {
  domains: WebApi.Domain[];
  query: Query;
  splat: string;
}

export default function Menu(props: Props) {
  const { domains, query, splat } = props;

  const navigateTo = useNavigate();

  const showDomain = React.useCallback(
    (domainPath: string) => {
      navigateTo({
        pathname: '/web_api/' + domainPath,
        search: queryToSearchString(serializeQuery(query)),
      });
    },
    [query, navigateTo],
  );

  const filteredDomains = (domains || [])
    .map((domain) => {
      const filteredActions = domain.actions.filter((action) =>
        actionsFilter(query, domain, action),
      );
      return { ...domain, filteredActions };
    })
    .filter((domain) => domain.filteredActions.length);

  const renderDomain = (domain: WebApi.Domain) => {
    const internal = !domain.actions.find((action) => !action.internal);

    return (
      <SubnavigationItem
        active={isDomainPathActive(domain.path, splat)}
        onClick={() => showDomain(domain.path)}
        key={domain.path}
      >
        {domain.path}
        {domain.deprecatedSince && <DeprecatedBadge since={domain.deprecatedSince} />}
        {internal && <InternalBadge />}
      </SubnavigationItem>
    );
  };

  return (
    <SubnavigationGroup className="sw-mt-4 sw-box-border">
      {filteredDomains.map(renderDomain)}
    </SubnavigationGroup>
  );
}
