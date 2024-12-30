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
import {
  Dropdown,
  InputMultiSelect,
  MultiSelector,
  PopupPlacement,
  PopupZLevel,
} from '~design-system';
import { Profile } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';

interface Props {
  inputId?: string;
  onChange: (selected: Profile[]) => void;
  profiles: Profile[];
  selectedProfiles: Profile[];
}

const LIST_SIZE = 0;

export function QualityProfileSelector(props: Readonly<Props>) {
  const { inputId, onChange, selectedProfiles, profiles } = props;

  const onSelect = React.useCallback(
    (selected: string) => {
      const profileFound = profiles.find(
        (profile) => `${profile.name} - ${profile.languageName}` === selected,
      );
      if (profileFound) {
        onChange([profileFound, ...selectedProfiles]);
      }
    },
    [profiles, onChange, selectedProfiles],
  );

  const onUnselect = React.useCallback(
    (selected: string) => {
      const selectedProfilesWithoutUnselected = selectedProfiles.filter(
        (profile) => `${profile.name} - ${profile.languageName}` !== selected,
      );
      onChange(selectedProfilesWithoutUnselected);
    },
    [onChange, selectedProfiles],
  );

  return (
    <Dropdown
      allowResizing
      closeOnClick={false}
      id="quality-profile-selector"
      overlay={
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions
        <div onMouseDown={handleMousedown}>
          <MultiSelector
            allowSearch={false}
            createElementLabel="" // Cannot create
            headerLabel={translate('coding_rules.select_profile')}
            noResultsLabel={translate('coding_rules.bulk_change.no_quality_profile')}
            onSelect={onSelect}
            onUnselect={onUnselect}
            searchInputAriaLabel={translate('search.search_for_profiles')}
            selectedElements={selectedProfiles.map(
              (profile) => `${profile.name} - ${profile.languageName}`,
            )}
            elements={profiles.map((profile) => `${profile.name} - ${profile.languageName}`)}
            listSize={LIST_SIZE}
          />
        </div>
      }
      placement={PopupPlacement.BottomLeft}
      zLevel={PopupZLevel.Global}
    >
      {({ onToggleClick }): JSX.Element => (
        <InputMultiSelect
          className="sw-w-full sw-mb-2"
          id={inputId}
          onClick={onToggleClick}
          placeholder={translate('select_verb')}
          selectedLabel={translate('coding_rules.selected_profiles')}
          count={selectedProfiles.length}
        />
      )}
    </Dropdown>
  );
}

/*
 * Prevent click from triggering a change of focus that would close the dropdown
 */
function handleMousedown(e: React.MouseEvent) {
  if ((e.target as HTMLElement).tagName !== 'INPUT') {
    e.preventDefault();
    e.stopPropagation();
  }
}
