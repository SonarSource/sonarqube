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
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import { hasMessage, translate } from 'sonar-ui-common/helpers/l10n';
import SelectList from '../../common/SelectList';
import SelectListItem from '../../common/SelectListItem';

export interface Props {
  fromHotspot: boolean;
  onSelect: (transition: string) => void;
  transitions: string[];
  type: T.IssueType;
}

export default function SetTransitionPopup({ fromHotspot, onSelect, transitions, type }: Props) {
  const isManualVulnerability = fromHotspot && type === 'VULNERABILITY';
  return (
    <DropdownOverlay>
      <SelectList currentItem={transitions[0]} items={transitions} onSelect={onSelect}>
        {transitions.map(transition => {
          const [name, description] = translateTransition(transition, isManualVulnerability);
          return (
            <SelectListItem item={transition} key={transition} title={description}>
              {name}
            </SelectListItem>
          );
        })}
      </SelectList>
    </DropdownOverlay>
  );
}

function translateTransition(transition: string, isManualVulnerability: boolean) {
  return isManualVulnerability && hasMessage('vulnerability.transition', transition)
    ? [
        translate('vulnerability.transition', transition),
        translate('vulnerability.transition', transition, 'description')
      ]
    : [
        translate('issue.transition', transition),
        translate('issue.transition', transition, 'description')
      ];
}
