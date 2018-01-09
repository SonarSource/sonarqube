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
import * as React from 'react';

interface Item {
  x: number;
  y: number;
  size: number;
  color?: string;
  key?: string;
  link?: any;
  tooltip?: string;
}

interface Props {
  items: Item[];
  sizeRange?: [number, number];
  displayXGrid?: boolean;
  displayXTicks?: boolean;
  displayYGrid?: boolean;
  displayYTicks?: boolean;
  height: number;
  padding: [number, number, number, number];
  formatXTick: (tick: number) => string;
  formatYTick: (tick: number) => string;
  onBubbleClick?: (link?: any) => void;
  xDomain?: [number, number];
  yDomain?: [number, number];
}

export default class BubbleChart extends React.Component<Props> {}
