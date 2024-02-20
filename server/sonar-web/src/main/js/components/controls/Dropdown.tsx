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
import * as React from 'react';
import { Popup, PopupPlacement } from '../ui/popups';
import ScreenPositionFixer from './ScreenPositionFixer';

interface OverlayProps {
  className?: string;
  children: React.ReactNode;
  noPadding?: boolean;
  placement?: PopupPlacement;
  useEventBoundary?: boolean;
}

export class DropdownOverlay extends React.Component<OverlayProps> {
  get placement() {
    return this.props.placement || PopupPlacement.Bottom;
  }

  renderPopup = (leftFix?: number, topFix?: number) => (
    <Popup
      arrowStyle={
        leftFix !== undefined && topFix !== undefined
          ? { transform: `translate(${-leftFix}px, ${-topFix}px)` }
          : undefined
      }
      className={this.props.className}
      noPadding={this.props.noPadding}
      placement={this.placement}
      style={
        leftFix !== undefined && topFix !== undefined
          ? { marginLeft: `calc(50% + ${leftFix}px)` }
          : undefined
      }
      useEventBoundary={this.props.useEventBoundary}
    >
      {this.props.children}
    </Popup>
  );

  render() {
    if (this.placement === PopupPlacement.Bottom) {
      return (
        <ScreenPositionFixer>
          {({ leftFix, topFix }) => this.renderPopup(leftFix, topFix)}
        </ScreenPositionFixer>
      );
    }
    return this.renderPopup();
  }
}
