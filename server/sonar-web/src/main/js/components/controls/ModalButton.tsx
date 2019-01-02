/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

export interface ChildrenProps {
  onClick: () => void;
  onFormSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

export interface ModalProps {
  onClose: () => void;
}

export interface Props {
  children: (props: ChildrenProps) => React.ReactNode;
  modal: (props: ModalProps) => React.ReactNode;
}

interface State {
  modal: boolean;
}

export default class ModalButton extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleButtonClick = () => {
    this.setState({ modal: true });
  };

  handleFormSubmit = (event?: React.FormEvent<HTMLFormElement>) => {
    if (event) {
      event.preventDefault();
    }
    this.setState({ modal: true });
  };

  handleCloseModal = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  render() {
    return (
      <>
        {this.props.children({
          onClick: this.handleButtonClick,
          onFormSubmit: this.handleFormSubmit
        })}
        {this.state.modal && this.props.modal({ onClose: this.handleCloseModal })}
      </>
    );
  }
}
