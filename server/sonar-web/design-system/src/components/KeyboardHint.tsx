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
import tw from 'twin.macro';
import { Key } from '../helpers/keyboard';
import { KeyboardHintKeys } from './KeyboardHintKeys';

interface Props {
  className?: string;
  command: string;
  title?: string;
}

export function KeyboardHint({ title, command, className }: Props) {
  const normalizedCommand = command
    .replace(Key.Control, isMacOS() ? 'Command' : 'Control')
    .replace(Key.Alt, isMacOS() ? 'Option' : 'Alt');

  return (
    <Body className={className}>
      {title && <span className="sw-truncate">{title}</span>}
      <KeyboardHintKeys command={normalizedCommand} />
    </Body>
  );
}

const Body = styled.div`
  ${tw`sw-flex sw-gap-2 sw-justify-center`}
  flex-wrap: wrap;
  color: var(--echoes-color-text-subdued);
`;

function isMacOS() {
  return navigator.userAgent.toLocaleLowerCase().includes('mac os');
}
