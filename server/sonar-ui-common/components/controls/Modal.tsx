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
import * as classNames from 'classnames';
import * as React from 'react';
import * as ReactModal from 'react-modal';
import { getReactDomContainerSelector } from '../../helpers/init';
import './Modal.css';

ReactModal.setAppElement(getReactDomContainerSelector());

export interface ModalProps {
  children: React.ReactNode;
  size?: 'small' | 'medium' | 'large';
  noBackdrop?: boolean;
}

interface Props extends ModalProps {
  /* String or object className to be applied to the modal content. */
  className?: string;

  /* String or object className to be applied to the overlay. */
  overlayClassName?: string;

  /* Function that will be run after the modal has opened. */
  onAfterOpen?(): void;

  /* Function that will be run after the modal has closed. */
  onAfterClose?(): void;

  /* Function that will be run when the modal is requested to be closed, prior to actually closing. */
  onRequestClose?(event: React.MouseEvent | React.KeyboardEvent): void;

  /* Boolean indicating if the modal should be focused after render */
  shouldFocusAfterRender?: boolean;

  /* Boolean indicating if the overlay should close the modal. Defaults to true. */
  shouldCloseOnOverlayClick?: boolean;

  /* Boolean indicating if pressing the esc key should close the modal */
  shouldCloseOnEsc?: boolean;

  /* String indicating how the content container should be announced to screenreaders. */
  contentLabel: string;
}

export default function Modal(props: Props) {
  return (
    <ReactModal
      className={classNames('modal', {
        'modal-small': props.size === 'small',
        'modal-medium': props.size === 'medium',
        'modal-large': props.size === 'large',
      })}
      isOpen={true}
      overlayClassName={classNames('modal-overlay', { 'modal-no-backdrop': props.noBackdrop })}
      {...props}
    />
  );
}
