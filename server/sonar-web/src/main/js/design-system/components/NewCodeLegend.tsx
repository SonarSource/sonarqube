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
import { themeColor } from '../helpers/theme';

export const NewCodeLegendIcon = styled.span`
  ${tw`sw-align-middle`}
  ${tw`sw-box-border`}
  ${tw`sw-h-4`}
  ${tw`sw-inline-block`}
  ${tw`sw-w-4`}
  background-color: ${themeColor('newCodeLegend')};
  border: 1px solid ${themeColor('newCodeLegendBorder')};
`;

const NewCodeLegendText = styled.span`
  ${tw`sw-align-middle`}
  ${tw`sw-typo-default`}
  ${tw`sw-ml-2`}
  color: var(--echoes-color-text-subdued);
`;

export function NewCodeLegend(props: Readonly<{ className?: string; text: string }>) {
  const { className, text } = props;

  return (
    <span className={classNames(className, 'sw-whitespace-nowrap')}>
      <NewCodeLegendIcon />
      <NewCodeLegendText>{text}</NewCodeLegendText>
    </span>
  );
}
