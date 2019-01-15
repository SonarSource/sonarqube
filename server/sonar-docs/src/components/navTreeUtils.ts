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
import { flatten } from 'lodash';
import {
  DocNavigationItem,
  DocsNavigationBlock,
  DocsNavigationExternalLink
} from '../@types/types';
import NavigationTree from '../../static/StaticNavigationTree.json';

export function getNavTree() {
  return NavigationTree as DocNavigationItem[];
}

export function getUrlsList(navTree: DocNavigationItem[]) {
  return flatten(
    navTree.map(leave => {
      if (isDocsNavigationBlock(leave)) {
        return leave.children;
      }
      if (isDocsNavigationExternalLink(leave)) {
        return leave.url;
      }
      return [leave];
    })
  );
}

export function isDocsNavigationBlock(leave?: DocNavigationItem): leave is DocsNavigationBlock {
  return typeof leave === 'object' && (leave as DocsNavigationBlock).children !== undefined;
}

export function isDocsNavigationExternalLink(
  leave?: DocNavigationItem
): leave is DocsNavigationExternalLink {
  return typeof leave === 'object' && (leave as DocsNavigationExternalLink).url !== undefined;
}
