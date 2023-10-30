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
import styled from '@emotion/styled';
import React from 'react';
import { registerListener, unregisterListener } from '../../helpers/globalMessages';
import { Message, MessageLevel } from '../../types/globalMessages';
import { zIndexes } from '../theme';
import GlobalMessage from './GlobalMessage';

const MESSAGE_DISPLAY_TIME = 10000;
const MAX_MESSAGES = 3;

interface State {
  messages: Message[];
}

export default class GlobalMessagesContainer extends React.Component<{}, State> {
  mounted = false;

  constructor(props: {}) {
    super(props);

    this.state = {
      messages: [],
    };
  }

  componentDidMount() {
    this.mounted = true;
    registerListener(this.handleAddMessage);
  }

  componentWillUnmount() {
    this.mounted = false;
    unregisterListener(this.handleAddMessage);
  }

  handleAddMessage = (message: Message) => {
    if (
      this.mounted &&
      !this.state.messages.some((m) => m.level === MessageLevel.Error && m.text === message.text)
    ) {
      this.setState(({ messages }) => ({
        messages: [...messages, message].slice(-MAX_MESSAGES),
      }));

      setTimeout(() => {
        this.closeMessage(message.id);
      }, MESSAGE_DISPLAY_TIME);
    }
  };

  closeMessage = (messageId: string) => {
    if (this.mounted) {
      this.setState(({ messages }) => {
        return { messages: messages.filter((m) => m.id !== messageId) };
      });
    }
  };

  render() {
    const { messages } = this.state;

    if (messages.length === 0) {
      return null;
    }

    return (
      <MessagesContainer>
        <div role="alert">
          {messages
            .filter((m) => m.level === MessageLevel.Error)
            .map((message) => (
              <GlobalMessage
                closeGlobalMessage={this.closeMessage}
                key={message.id}
                message={message}
              />
            ))}
        </div>
        <output>
          {messages
            .filter((m) => m.level === MessageLevel.Success)
            .map((message) => (
              <GlobalMessage
                closeGlobalMessage={this.closeMessage}
                key={message.id}
                message={message}
              />
            ))}
        </output>
      </MessagesContainer>
    );
  }
}

const MessagesContainer = styled.div`
  position: fixed;
  z-index: ${zIndexes.processContainerZIndex};
  top: 0;
  left: 50%;
  width: 350px;
  margin-left: -175px;
  z-index: 8600;
`;
