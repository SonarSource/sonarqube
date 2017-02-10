/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import Action from './Action';
import InternalBadge from './InternalBadge';
import { getActionKey } from '../utils';

export default function Domain ({ domain, showInternal, showOnlyDeprecated, searchQuery }) {
  const filteredActions = domain.actions
      .filter(action => {
        return showInternal || !action.internal;
      })
      .filter(action => {
        return !showOnlyDeprecated || (showOnlyDeprecated && action.deprecatedSince);
      })
      .filter(action => {
        const actionKey = getActionKey(domain.path, action.key);
        return actionKey.indexOf(searchQuery) !== -1;
      });

  return (
      <div className="web-api-domain">
        <header className="web-api-domain-header">
          <h2 className="web-api-domain-title">{domain.path}</h2>

          {domain.internal && (
              <span className="spacer-left">
                <InternalBadge/>
              </span>
          )}
        </header>

        {domain.description && (
            <p className="web-api-domain-description">{domain.description}</p>
        )}

        <div className="web-api-domain-actions">
          {filteredActions.map(action => (
              <Action
                  key={getActionKey(domain.path, action.key)}
                  action={action}
                  domain={domain}
                  location={location}
                  showInternal={showInternal}/>
          ))}
        </div>
      </div>
  );
}
