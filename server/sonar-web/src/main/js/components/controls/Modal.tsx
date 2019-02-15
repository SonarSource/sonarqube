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
import * as ReactModal from 'react-modal';
import * as classNames from 'classnames';

ReactModal.setAppElement('#content');

export interface ModalProps {
  children: React.ReactNode;
  size?: 'small' | 'medium' | 'large';
  noBackdrop?: boolean;
}

type MandatoryProps = Pick<ReactModal.Props, 'contentLabel'>;

type Props = Partial<ReactModal.Props> & MandatoryProps & ModalProps;

export default function Modal(props: Props) {
  return (
    <ReactModal
      className={classNames('modal', {
        'modal-small': props.size === 'small',
        'modal-medium': props.size === 'medium',
        'modal-large': props.size === 'large'
      })}
      isOpen={true}
      overlayClassName={classNames('modal-overlay', { 'modal-no-backdrop': props.noBackdrop })}
      {...props}
    />
  );
}
