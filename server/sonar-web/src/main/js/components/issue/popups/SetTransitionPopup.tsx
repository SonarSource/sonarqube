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
import { FormattedMessage } from 'react-intl';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import { translate } from '../../../helpers/l10n';
import Link from '../../common/Link';
import SelectList from '../../common/SelectList';
import SelectListItem from '../../common/SelectListItem';

export interface Props {
  onSelect: (transition: string) => void;
  transitions: string[];
}

export default function SetTransitionPopup({ onSelect, transitions }: Props) {
  return (
    <DropdownOverlay>
      <SelectList currentItem={transitions[0]} items={transitions} onSelect={onSelect}>
        {transitions.map((transition) => {
          const [name, description] = translateTransition(transition);
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

function translateTransition(transition: string) {
  return [
    translate('issue.transition', transition),
    <FormattedMessage
      key="description"
      defaultMessage={translate('issue.transition', transition, 'description')}
      id={`issue.transition.${transition}.description`}
      values={{
        community_plug_link: (
          <Link to="https://community.sonarsource.com/" target="_blank">
            {translate('issue.transition.community_plug_link')}
          </Link>
        ),
      }}
    />,
  ];
}
