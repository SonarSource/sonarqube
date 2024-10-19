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
import styled from '@emotion/styled';
import classNames from 'classnames';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers';
import { Key } from '../helpers/keyboard';
import { TriangleDownIcon, TriangleLeftIcon, TriangleRightIcon, TriangleUpIcon } from './icons';

export const mappedKeys = {
  [Key.Alt]: 'Alt',
  [Key.ArrowDown]: <TriangleDownIcon />,
  [Key.ArrowLeft]: <TriangleLeftIcon />,
  [Key.ArrowRight]: <TriangleRightIcon />,
  [Key.ArrowUp]: <TriangleUpIcon />,
  [Key.Command]: '⌘',
  [Key.Control]: 'Ctrl',
  [Key.Option]: '⌥',
  [Key.Click]: 'click',
};

const NON_KEY_SYMBOLS = ['+', ' '];

export function KeyboardHintKeys({ command }: { command: string }) {
  const keys = command
    .trim()
    .split(' ')
    .map((key, index) => {
      const uniqueKey = `${key}-${index}`;

      if (NON_KEY_SYMBOLS.includes(key)) {
        return <span key={uniqueKey}>{key}</span>;
      }

      const isNonMappedKey = !(
        Object.keys(mappedKeys).includes(key) || Object.values(mappedKeys).includes(key)
      );

      return (
        <KeyBox className={classNames({ 'sw-px-1': isNonMappedKey })} key={uniqueKey}>
          {Object.keys(mappedKeys).includes(key) ? mappedKeys[key as keyof typeof mappedKeys] : key}
        </KeyBox>
      );
    });

  return <div className="sw-flex sw-gap-1">{keys}</div>;
}

export const KeyBox = styled.span`
  ${tw`sw-flex sw-items-center sw-justify-center`}
  ${tw`sw-px-1/2`}
  ${tw`sw-rounded-1/2`}

  background-color: ${themeColor('keyboardHintKey')};
  color: ${themeContrast('keyboardHintKey')};
`;
