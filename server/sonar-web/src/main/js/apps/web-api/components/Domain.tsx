/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import Action from './Action';
import DeprecatedBadge from './DeprecatedBadge';
import InternalBadge from './InternalBadge';
import { getActionKey, actionsFilter } from '../utils';
import { Domain as DomainType } from '../../../api/web-api';

interface Props {
  domain: DomainType;
  showDeprecated: boolean;
  showInternal: boolean;
  searchQuery: string;
}

export default function Domain({ domain, showInternal, showDeprecated, searchQuery }: Props) {
  const filteredActions = domain.actions.filter(action =>
    actionsFilter(showDeprecated, showInternal, searchQuery, domain, action)
  );

  return (
    <div className="web-api-domain">
      <header className="web-api-domain-header">
        <h2 className="web-api-domain-title">{domain.path}</h2>

        {domain.deprecated && (
          <span className="spacer-left">
            <DeprecatedBadge />
          </span>
        )}

        {domain.internal && (
          <span className="spacer-left">
            <InternalBadge />
          </span>
        )}
      </header>

      {domain.description && (
        <div
          className="web-api-domain-description markdown"
          dangerouslySetInnerHTML={{ __html: domain.description }}
        />
      )}

      <div className="web-api-domain-actions">
        {filteredActions.map(action => (
          <Action
            key={getActionKey(domain.path, action.key)}
            action={action}
            domain={domain}
            showDeprecated={showDeprecated}
            showInternal={showInternal}
          />
        ))}
      </div>
    </div>
  );
}
