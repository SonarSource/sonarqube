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
import { SubTitle } from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { WebApi } from '../../../types/types';
import { Query, actionsFilter, getActionKey } from '../utils';
import Action from './Action';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';

interface Props {
  domain: WebApi.Domain;
  query: Query;
}

export default function Domain({ domain, query }: Props) {
  const filteredActions = domain.actions.filter((action) => actionsFilter(query, domain, action));

  return (
    <div>
      <header className="sw-flex sw-items-baseline sw-gap-3">
        <SubTitle className="sw-m-0">{domain.path}</SubTitle>

        {!isEmpty(domain.deprecatedSince) && <DeprecatedBadge since={domain.deprecatedSince} />}

        {domain.internal && <InternalBadge />}
      </header>

      {!isEmpty(domain.description) && (
        <div
          className="sw-mt-3 markdown"
          // Safe: comes from the backend
          dangerouslySetInnerHTML={{ __html: domain.description }}
        />
      )}

      <div className="sw-mt-4 sw-flex sw-flex-col sw-gap-4">
        {filteredActions.map((action) => (
          <Action
            action={action}
            domain={domain}
            key={getActionKey(domain.path, action.key)}
            showDeprecated={query.deprecated}
            showInternal={query.internal}
          />
        ))}
      </div>
    </div>
  );
}
