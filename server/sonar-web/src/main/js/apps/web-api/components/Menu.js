/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { Link } from 'react-router';
import classNames from 'classnames';
import InternalBadge from './InternalBadge';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { getActionKey, isDomainPathActive } from '../utils';

export default function Menu ({ domains, showInternal, showOnlyDeprecated, searchQuery, splat }) {
  const filteredDomains = (domains || [])
      .map(domain => {
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
        return { ...domain, filteredActions };
      })
      .filter(domain => domain.filteredActions.length);

  return (
      <div className="api-documentation-results panel">
        <TooltipsContainer>
          <div className="list-group">
            {filteredDomains.map(domain => (
                <Link
                    key={domain.path}
                    className={classNames('list-group-item', { 'active': isDomainPathActive(domain.path, splat) })}
                    to={'/web_api/' + domain.path}>
                  <h3 className="list-group-item-heading">
                    {domain.path}
                    {domain.internal && (
                        <InternalBadge/>
                    )}
                  </h3>
                  <p className="list-group-item-text">
                    {domain.description}
                  </p>
                </Link>
            ))}
          </div>
        </TooltipsContainer>
      </div>
  );
}
