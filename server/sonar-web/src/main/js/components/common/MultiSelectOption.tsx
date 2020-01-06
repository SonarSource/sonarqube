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
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface MultiSelectOptionProps {
  active?: boolean;
  custom?: boolean;
  disabled?: boolean;
  element: string;
  onHover: (element: string) => void;
  onSelectChange: (selected: boolean, element: string) => void;
  renderLabel: (element: string) => React.ReactNode;
  selected?: boolean;
}

export default function MultiSelectOption(props: MultiSelectOptionProps) {
  const { active, custom, disabled, element, selected } = props;
  const onHover = () => props.onHover(element);
  const className = classNames({ active, disabled });
  const label = props.renderLabel(element);

  return (
    <li onFocus={onHover} onMouseOver={onHover}>
      <Checkbox
        checked={Boolean(selected)}
        className={className}
        disabled={disabled}
        id={element}
        onCheck={props.onSelectChange}>
        {custom ? (
          <span
            aria-label={`${translate('create_new_element')}: ${label}`}
            className="little-spacer-left">
            <span aria-hidden={true} className="little-spacer-right">
              +
            </span>
            {label}
          </span>
        ) : (
          <span className="little-spacer-left">{label}</span>
        )}
      </Checkbox>
    </li>
  );
}
