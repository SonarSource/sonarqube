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
import classNames from 'classnames';
import * as React from 'react';
import { DropdownOverlay } from '../../../../components/controls/Dropdown';
import SearchBox from '../../../../components/controls/SearchBox';
import Avatar from '../../../../components/ui/Avatar';
import DeferredSpinner from '../../../../components/ui/DeferredSpinner';
import { PopupPlacement } from '../../../../components/ui/popups';
import { translate } from '../../../../helpers/l10n';
import { UserActive } from '../../../../types/users';
import './AssigneeSelection.css';

export interface HotspotAssigneeSelectRendererProps {
  highlighted?: UserActive;
  loading: boolean;
  onKeyDown: (event: React.KeyboardEvent) => void;
  onSearch: (query: string) => void;
  onSelect: (user?: UserActive) => void;
  query?: string;
  suggestedUsers?: UserActive[];
}

export default function AssigneeSelectionRenderer(props: HotspotAssigneeSelectRendererProps) {
  const { highlighted, loading, query, suggestedUsers } = props;

  return (
    <div className="dropdown">
      <div className="display-flex-center">
        <SearchBox
          autoFocus={true}
          onChange={props.onSearch}
          onKeyDown={props.onKeyDown}
          placeholder={translate('hotspots.assignee.select_user')}
          value={query}
        />
        {loading && <DeferredSpinner className="spacer-left" />}
      </div>

      {!loading && (
        <DropdownOverlay noPadding={true} placement={PopupPlacement.BottomLeft}>
          <ul className="hotspot-assignee-search-results">
            {suggestedUsers &&
              suggestedUsers.map((suggestion) => (
                <li
                  className={classNames('padded', {
                    active: highlighted && highlighted.login === suggestion.login,
                  })}
                  key={suggestion.login}
                  onClick={() => props.onSelect(suggestion)}
                >
                  {suggestion.login && (
                    <Avatar
                      className="spacer-right"
                      hash={suggestion.avatar}
                      name={suggestion.name}
                      size={16}
                    />
                  )}
                  {suggestion.name}
                </li>
              ))}
          </ul>
        </DropdownOverlay>
      )}
    </div>
  );
}
