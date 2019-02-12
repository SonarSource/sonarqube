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
import ConfirmModal, { ConfirmModalProps } from './ConfirmModal';
import ModalButton, { ChildrenProps, ModalProps } from './ModalButton';

interface Props<T> extends ConfirmModalProps<T> {
  children: (props: ChildrenProps) => React.ReactNode;
  modalBody: React.ReactNode;
  modalHeader: string;
  modalHeaderDescription?: React.ReactNode;
}

interface State {
  modal: boolean;
}

export default class ConfirmButton<T> extends React.PureComponent<Props<T>, State> {
  renderConfirmModal = ({ onClose }: ModalProps) => {
    const {
      children,
      modalBody,
      modalHeader,
      modalHeaderDescription,
      ...confirmModalProps
    } = this.props;
    return (
      <ConfirmModal
        header={modalHeader}
        headerDescription={modalHeaderDescription}
        onClose={onClose}
        {...confirmModalProps}>
        {modalBody}
      </ConfirmModal>
    );
  };

  render() {
    return <ModalButton modal={this.renderConfirmModal}>{this.props.children}</ModalButton>;
  }
}
