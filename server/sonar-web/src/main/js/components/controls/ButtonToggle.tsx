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
import { Button } from './buttons';
import './ButtonToggle.css';

export type ButtonToggleValueType = string | number | boolean;

export interface ButtonToggleOption {
  label: string;
  value: ButtonToggleValueType;
}

export interface ButtonToggleProps {
  label?: string;
  disabled?: boolean;
  options: ButtonToggleOption[];
  value?: ButtonToggleValueType;
  onCheck: (value: ButtonToggleValueType) => void;
}

export default function ButtonToggle(props: ButtonToggleProps) {
  const { disabled, label, options, value } = props;

  return (
    <ul aria-label={label} className="button-toggle">
      {options.map((option) => (
        <li key={option.value.toString()}>
          <Button
            onClick={() => option.value !== value && props.onCheck(option.value)}
            disabled={disabled}
            aria-current={option.value === value}
            data-value={option.value}
            className={classNames({ selected: option.value === value })}
          >
            {option.label}
          </Button>
        </li>
      ))}
    </ul>
  );
}
