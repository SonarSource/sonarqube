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
import { identity } from 'lodash';
import { PopupPlacement } from '../../helpers';
import { ItemCheckbox } from '../DropdownMenu';
import { Tooltip } from '../Tooltip';

export interface MultiSelectOptionProps {
  active?: boolean;
  createElementLabel: string;
  custom?: boolean;
  disabled?: boolean;
  element: string;
  onHover: (element: string) => void;
  onSelectChange: (selected: boolean, element: string) => void;
  renderLabel?: (element: string) => React.ReactNode;
  renderTooltip?: (element: string, disabled: boolean) => React.ReactNode;
  selected?: boolean;
}

export function MultiSelectMenuOption(props: MultiSelectOptionProps) {
  const {
    active,
    createElementLabel,
    custom,
    disabled = false,
    element,
    onSelectChange,
    selected,
    renderLabel = identity,
    renderTooltip,
  } = props;

  const onHover = () => {
    props.onHover(element);
  };

  const label = renderLabel(element);

  return (
    <Tooltip overlay={renderTooltip?.(element, disabled)} placement={PopupPlacement.Right}>
      <ItemCheckbox
        checked={Boolean(selected)}
        className={classNames('sw-flex sw-py-2 sw-px-4', { active })}
        disabled={disabled}
        id={element}
        label={element}
        onCheck={onSelectChange}
        onFocus={onHover}
        onPointerEnter={onHover}
      >
        {custom ? (
          <span
            aria-label={`${createElementLabel}: ${element}`}
            className="sw-ml-3"
            title={createElementLabel}
          >
            <span aria-hidden className="sw-mr-1">
              +
            </span>
            {element}
          </span>
        ) : (
          <span className="sw-ml-3">{label}</span>
        )}
      </ItemCheckbox>
    </Tooltip>
  );
}
