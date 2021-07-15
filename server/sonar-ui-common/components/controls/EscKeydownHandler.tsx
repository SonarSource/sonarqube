/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { KeyCodes } from '../../helpers/keycodes';

interface Props {
  children: React.ReactNode;
  onKeydown: () => void;
}

export default class EscKeydownHandler extends React.Component<Props> {
  componentDidMount() {
    setTimeout(() => {
      document.addEventListener('keydown', this.handleKeyDown, false);
    }, 0);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleKeyDown, false);
  }

  handleKeyDown = (event: KeyboardEvent) => {
    if (event.keyCode === KeyCodes.Escape) {
      this.props.onKeydown();
    }
  };

  render() {
    return this.props.children;
  }
}
