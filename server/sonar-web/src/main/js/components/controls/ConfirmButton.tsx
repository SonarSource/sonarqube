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
import ModalButton, { ChildrenProps, ModalProps } from './ModalButton';
import ConfirmModal from './ConfirmModal';

export { ChildrenProps } from './ModalButton';

interface Props {
  children: (props: ChildrenProps) => React.ReactNode;
  cancelButtonText?: string;
  confirmButtonText: string;
  confirmData?: string;
  confirmDisable?: boolean;
  isDestructive?: boolean;
  modalBody: React.ReactNode;
  modalHeader: string;
  onConfirm: (data?: string) => void | Promise<void>;
}

interface State {
  modal: boolean;
}

export default class ConfirmButton extends React.PureComponent<Props, State> {
  renderConfirmModal = ({ onClose }: ModalProps) => {
    return (
      <ConfirmModal
        cancelButtonText={this.props.cancelButtonText}
        confirmButtonText={this.props.confirmButtonText}
        confirmData={this.props.confirmData}
        confirmDisable={this.props.confirmDisable}
        header={this.props.modalHeader}
        isDestructive={this.props.isDestructive}
        onClose={onClose}
        onConfirm={this.props.onConfirm}>
        {this.props.modalBody}
      </ConfirmModal>
    );
  };

  render() {
    return <ModalButton modal={this.renderConfirmModal}>{this.props.children}</ModalButton>;
  }
}
