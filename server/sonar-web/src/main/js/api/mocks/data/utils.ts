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

import { flatMap, map } from 'lodash';
import { Component } from '../../../types/types';
import { ComponentTree } from './components';

export function isLeaf(node: ComponentTree) {
  return node.children.length === 0;
}

export function listChildComponent(node: ComponentTree): Component[] {
  return map(node.children, (n) => n.component);
}

export function listAllComponent(node: ComponentTree): Component[] {
  if (isLeaf(node)) {
    return [node.component];
  }

  return [node.component, ...flatMap(node.children, listAllComponent)];
}

export function listAllComponentTrees(node: ComponentTree): ComponentTree[] {
  if (isLeaf(node)) {
    return [node];
  }

  return [node, ...flatMap(node.children, listAllComponentTrees)];
}

export function listLeavesComponent(node: ComponentTree): Component[] {
  if (isLeaf(node)) {
    return [node.component];
  }
  return flatMap(node.children, listLeavesComponent);
}
