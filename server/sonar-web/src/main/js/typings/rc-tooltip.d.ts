/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
declare module 'rc-tooltip' {
  export type Trigger = 'hover' | 'click' | 'focus';
  export type Placement =
    | 'left'
    | 'right'
    | 'top'
    | 'bottom'
    | 'topLeft'
    | 'topRight'
    | 'bottomLeft'
    | 'bottomRight';

  export interface Props extends React.Props<any> {
    overlayClassName?: string;
    trigger?: Trigger[];
    mouseEnterDelay?: number;
    mouseLeaveDelay?: number;
    overlayStyle?: React.CSSProperties;
    prefixCls?: string;
    transitionName?: string;
    onVisibleChange?: () => void;
    visible?: boolean;
    defaultVisible?: boolean;
    placement?: Placement | Object;
    align?: Object;
    onPopupAlign?: (popupDomNode: Element, align: Object) => void;
    overlay: React.ReactNode;
    arrowContent?: React.ReactNode;
    getTooltipContainer?: () => Element;
    destroyTooltipOnHide?: boolean;
  }

  // the next line is crucial, it is absent in the original typings
  export default class Tooltip extends React.Component<Props> {}
}
