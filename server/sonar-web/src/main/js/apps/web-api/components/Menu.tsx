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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import { isDomainPathActive, actionsFilter, Query, serializeQuery } from '../utils';

interface Props {
  domains: T.WebApi.Domain[];
  query: Query;
  splat: string;
}

export default function Menu(props: Props) {
  const { domains, query, splat } = props;
  const filteredDomains = (domains || [])
    .map(domain => {
      const filteredActions = domain.actions.filter(action => actionsFilter(query, domain, action));
      return { ...domain, filteredActions };
    })
    .filter(domain => domain.filteredActions.length);

  const renderDomain = (domain: T.WebApi.Domain) => {
    const internal = !domain.actions.find(action => !action.internal);
    return (
      <Link
        className={classNames('list-group-item', {
          active: isDomainPathActive(domain.path, splat)
        })}
        key={domain.path}
        to={{ pathname: '/web_api/' + domain.path, query: serializeQuery(query) }}>
        <h3 className="list-group-item-heading">
          {domain.path}
          {domain.deprecatedSince && <DeprecatedBadge since={domain.deprecatedSince} />}
          {internal && <InternalBadge />}
        </h3>
      </Link>
    );
  };

  return (
    <div className="api-documentation-results panel">
      <div className="list-group">{filteredDomains.map(renderDomain)}</div>
    </div>
  );
}
