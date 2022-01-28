/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { colors, sizes, zIndexes } from '../../app/theme';
import { cutLongWords } from '../../helpers/path';
import { ClearButton } from './buttons';

interface IMessage {
  id: string;
  level: 'ERROR' | 'SUCCESS';
  message: string;
}

export interface GlobalMessagesProps {
  closeGlobalMessage: (id: string) => void;
  messages: IMessage[];
}

export default function GlobalMessages({ closeGlobalMessage, messages }: GlobalMessagesProps) {
  if (messages.length === 0) {
    return null;
  }

  return (
    <MessagesContainer>
      {messages.map(message => (
        <GlobalMessage closeGlobalMessage={closeGlobalMessage} key={message.id} message={message} />
      ))}
    </MessagesContainer>
  );
}

const MessagesContainer = styled.div`
  position: fixed;
  z-index: ${zIndexes.processContainerZIndex};
  top: 0;
  left: 50%;
  width: 350px;
  margin-left: -175px;
`;

export class GlobalMessage extends React.PureComponent<{
  closeGlobalMessage: (id: string) => void;
  message: IMessage;
}> {
  handleClose = () => {
    this.props.closeGlobalMessage(this.props.message.id);
  };

  render() {
    const { message } = this.props;
    return (
      <Message
        data-test={`global-message__${message.level}`}
        level={message.level}
        role={message.level === 'SUCCESS' ? 'status' : 'alert'}>
        {cutLongWords(message.message)}
        <CloseButton
          className="button-small"
          color="#fff"
          level={message.level}
          onClick={this.handleClose}
        />
      </Message>
    );
  }
}

const appearAnim = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`;

const Message = styled.div<Pick<IMessage, 'level'>>`
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

  & + & {
    margin-top: calc(${sizes.gridSize} / 2);
    border-radius: 3px;
  }
`;

const CloseButton = styled(ClearButton)<Pick<IMessage, 'level'>>`
  position: absolute;
  top: calc(${sizes.gridSize} / 4);
  right: calc(${sizes.gridSize} / 4);

  &:hover svg,
  &:focus svg {
    color: ${({ level }) => (level === 'SUCCESS' ? colors.green : colors.red)};
  }
`;
