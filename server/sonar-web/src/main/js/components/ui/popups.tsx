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
import * as classNames from 'classnames';
import './popups.css';

export enum PopupPlacement {
  Bottom = 'bottom',
  BottomLeft = 'bottom-left',
  BottomRight = 'bottom-right',
  LeftTop = 'left-top',
  RightTop = 'right-top'
}

interface PopupProps {
  arrowStyle?: React.CSSProperties;
  children?: React.ReactNode;
  className?: string;
  noPadding?: boolean;
  placement?: PopupPlacement;
  style?: React.CSSProperties;
}

export function Popup(props: PopupProps) {
  const { placement = PopupPlacement.Bottom } = props;
  return (
    <div
      className={classNames(
        'popup',
        `is-${placement}`,
        { 'no-padding': props.noPadding },
        props.className
      )}
      style={props.style}>
      {props.children}
      <PopupArrow style={props.arrowStyle} />
    </div>
  );
}

interface PopupArrowProps {
  style?: React.CSSProperties;
}

export function PopupArrow(props: PopupArrowProps) {
  return <div className="popup-arrow" style={props.style} />;
}
