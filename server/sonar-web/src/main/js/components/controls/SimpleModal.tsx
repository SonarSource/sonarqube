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
import Modal, { ModalProps } from './Modal';

export interface ChildrenProps {
  onCloseClick: (event?: React.SyntheticEvent<HTMLElement>) => void;
  onFormSubmit: (event: React.SyntheticEvent<HTMLFormElement>) => void;
  onSubmitClick: (event?: React.SyntheticEvent<HTMLElement>) => void;
  submitting: boolean;
}

interface Props extends ModalProps {
  children: (props: ChildrenProps) => React.ReactNode;
  header: string;
  onClose: () => void;
  onSubmit: () => void | Promise<void>;
}

interface State {
  submitting: boolean;
}

export default class SimpleModal extends React.Component<Props, State> {
  mounted = false;
  state: State = { submitting: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  handleCloseClick = (event?: React.SyntheticEvent<HTMLElement>) => {
    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }
    this.props.onClose();
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.submit();
  };

  handleSubmitClick = (event?: React.SyntheticEvent<HTMLElement>) => {
    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }
    this.submit();
  };

  submit = () => {
    const result = this.props.onSubmit();
    if (result) {
      this.setState({ submitting: true });
      result.then(this.stopSubmitting, this.stopSubmitting);
    }
  };

  render() {
    const { children, header, onClose, onSubmit, ...modalProps } = this.props;
    return (
      <Modal contentLabel={header} onRequestClose={onClose} {...modalProps}>
        {children({
          onCloseClick: this.handleCloseClick,
          onFormSubmit: this.handleFormSubmit,
          onSubmitClick: this.handleSubmitClick,
          submitting: this.state.submitting
        })}
      </Modal>
    );
  }
}
