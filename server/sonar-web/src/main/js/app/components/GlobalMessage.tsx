/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import * as React from 'react';
import { ClearButton } from '../../components/controls/buttons';
import { cutLongWords } from '../../helpers/path';
import { Message } from '../../types/globalMessages';
import { colors, sizes } from '../theme';

export interface GlobalMessageProps {
  closeGlobalMessage: (id: string) => void;
  message: Message;
}

export default function GlobalMessage(props: GlobalMessageProps) {
  const { message } = props;
  return (
    <MessageBox data-testid={`global-message__${message.level}`} level={message.level}>
      {cutLongWords(message.text)}
      <CloseButton
        className="button-small"
        color="#fff"
        level={message.level}
        onClick={() => props.closeGlobalMessage(message.id)}
      />
    </MessageBox>
  );
}

const appearAnim = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`;

const MessageBox = styled.div<Pick<Message, 'level'>>`
  position: relative;
  padding: 0 30px 0 10px;
  line-height: ${sizes.controlHeight};
  border-radius: 0 0 3px 3px;
  box-sizing: border-box;
  color: #ffffff;
  background-color: ${({ level }) => (level === 'SUCCESS' ? colors.green : colors.red)};
  text-align: center;
  opacity: 0;
  animation: ${appearAnim} 0.2s ease forwards;

  margin-top: calc(${sizes.gridSize} / 2);
  border-radius: 3px;
`;

const CloseButton = styled(ClearButton)<Pick<Message, 'level'>>`
  position: absolute;
  top: calc(${sizes.gridSize} / 4);
  right: calc(${sizes.gridSize} / 4);

  &.button-icon:hover svg,
  &.button-icon:focus svg {
    color: ${({ level }) => (level === 'SUCCESS' ? colors.green : colors.red)};
  }
`;
