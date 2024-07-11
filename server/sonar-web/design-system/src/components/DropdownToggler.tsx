/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { FocusOutHandler } from './FocusOutHandler';
import { OutsideClickHandler } from './OutsideClickHandler';
import { Popup } from './popups';

type PopupProps = Popup['props'];

interface Props extends PopupProps {
  onRequestClose: VoidFunction;
  open: boolean;
  withClickOutHandler?: boolean;
  withFocusOutHandler?: boolean;
}

/** @deprecated Use DropdownMenu.Root and other DropdownMenu.* elements from Echoes instead.
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3354918914/DropdownMenus | Migration Guide}
 */
export function DropdownToggler(props: Props) {
  const {
    children,
    open,
    onRequestClose,
    withClickOutHandler = true,
    withFocusOutHandler = true,
    overlay,
    ...popupProps
  } = props;

  let finalOverlay = <EscKeydownHandler onKeydown={onRequestClose}>{overlay}</EscKeydownHandler>;

  if (withFocusOutHandler) {
    finalOverlay = <FocusOutHandler onFocusOut={onRequestClose}>{finalOverlay}</FocusOutHandler>;
  }

  if (withClickOutHandler) {
    finalOverlay = (
      <OutsideClickHandler onClickOutside={onRequestClose}>{finalOverlay}</OutsideClickHandler>
    );
  }

  return (
    <Popup overlay={open && finalOverlay} {...popupProps}>
      {children}
    </Popup>
  );
}
