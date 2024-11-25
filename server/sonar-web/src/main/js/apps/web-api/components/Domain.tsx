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
import { SafeHTMLInjection } from '../../../helpers/sanitize';
import { WebApi } from '../../../types/types';
import { actionsFilter, getActionKey, Query } from '../utils';
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
    <div className="web-api-domain">
      <header className="web-api-domain-header">
        <h2 className="web-api-domain-title">{domain.path}</h2>

        {domain.deprecatedSince && (
          <span className="spacer-left">
            <DeprecatedBadge since={domain.deprecatedSince} />
          </span>
        )}

        {domain.internal && (
          <span className="spacer-left">
            <InternalBadge />
          </span>
        )}
      </header>

      {domain.description && (
        <SafeHTMLInjection htmlAsString={domain.description}>
          <div className="web-api-domain-description markdown" />
        </SafeHTMLInjection>
      )}

      <div className="web-api-domain-actions">
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
