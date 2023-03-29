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
import EscKeydownHandler from './EscKeydownHandler';
import OutsideClickHandler from './OutsideClickHandler';
import { Popup } from './popups';

type PopupProps = Popup['props'];

interface Props extends PopupProps {
  onRequestClose: VoidFunction;
  open: boolean;
}

export default function DropdownToggler(props: Props) {
  const { children, open, onRequestClose, overlay, ...popupProps } = props;

  return (
    <Popup
      overlay={
        open ? (
          <OutsideClickHandler onClickOutside={onRequestClose}>
            <EscKeydownHandler onKeydown={onRequestClose}>{overlay}</EscKeydownHandler>
          </OutsideClickHandler>
        ) : undefined
      }
      {...popupProps}
    >
      {children}
    </Popup>
  );
}
