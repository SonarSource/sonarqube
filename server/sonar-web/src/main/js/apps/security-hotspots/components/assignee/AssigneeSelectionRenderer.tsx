/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { PopupPlacement } from 'sonar-ui-common/components/ui/popups';
import { translate } from 'sonar-ui-common/helpers/l10n';
import Avatar from '../../../../components/ui/Avatar';
import './AssigneeSelection.css';

export interface HotspotAssigneeSelectRendererProps {
  highlighted?: T.UserActive;
  loading: boolean;
  onKeyDown: (event: React.KeyboardEvent) => void;
  onSearch: (query: string) => void;
  onSelect: (user: T.UserActive) => void;
  open: boolean;
  query?: string;
  suggestedUsers?: T.UserActive[];
}

export default function AssigneeSelectionRenderer(props: HotspotAssigneeSelectRendererProps) {
  const { highlighted, loading, open, query, suggestedUsers } = props;
  return (
    <>
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

      {!loading && open && (
        <div className="position-relative">
          <DropdownOverlay noPadding={true} placement={PopupPlacement.BottomLeft}>
            {suggestedUsers && suggestedUsers.length > 0 ? (
              <ul className="hotspot-assignee-search-results">
                {suggestedUsers.map(suggestion => (
                  <li
                    className={classNames('padded', {
                      active: highlighted && highlighted.login === suggestion.login
                    })}
                    key={suggestion.login}
                    onClick={() => props.onSelect(suggestion)}>
                    <Avatar
                      className="spacer-right"
                      hash={suggestion.avatar}
                      name={suggestion.name}
                      size={16}
                    />
                    {suggestion.name}
                  </li>
                ))}
              </ul>
            ) : (
              <div className="padded">{translate('no_results')}</div>
            )}
          </DropdownOverlay>
        </div>
      )}
    </>
  );
}
