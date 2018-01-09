/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

interface RenderProps {
  closeDropdown: () => void;
  onToggleClick: (event: React.SyntheticEvent<HTMLElement>) => void;
  open: boolean;
}

interface Props {
  children: (renderProps: RenderProps) => JSX.Element;
  onOpen?: () => void;
}

interface State {
  open: boolean;
}

export default class Dropdown extends React.PureComponent<Props, State> {
  toggleNode?: HTMLElement;

  constructor(props: Props) {
    super(props);
    this.state = { open: false };
  }

  componentDidUpdate(_: Props, prevState: State) {
    if (!prevState.open && this.state.open) {
      this.addClickHandler();
      if (this.props.onOpen) {
        this.props.onOpen();
      }
    }

    if (prevState.open && !this.state.open) {
      this.removeClickHandler();
    }
  }

  componentWillUnmount() {
    this.removeClickHandler();
  }

  addClickHandler = () => {
    window.addEventListener('click', this.handleWindowClick);
  };

  removeClickHandler = () => {
    window.removeEventListener('click', this.handleWindowClick);
  };

  handleWindowClick = (event: MouseEvent) => {
    if (!this.toggleNode || !this.toggleNode.contains(event.target as Node)) {
      this.closeDropdown();
    }
  };

  closeDropdown = () => this.setState({ open: false });

  handleToggleClick = (event: React.SyntheticEvent<HTMLElement>) => {
    this.toggleNode = event.currentTarget;
    event.preventDefault();
    event.currentTarget.blur();
    this.setState(state => ({ open: !state.open }));
  };

  render() {
    return this.props.children({
      closeDropdown: this.closeDropdown,
      onToggleClick: this.handleToggleClick,
      open: this.state.open
    });
  }
}
