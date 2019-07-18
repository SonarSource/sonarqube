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
import NavigationTree from '../../static/StaticNavigationTree.json';
import {
  DocNavigationItem,
  DocsNavigationBlock,
  DocsNavigationExternalLink
} from '../@types/types';

export function getNavTree() {
  return NavigationTree as DocNavigationItem[];
}

export function getUrlsList(navTree: DocNavigationItem[]): string[] {
  return flatten(
    navTree.map(leaf => {
      if (isDocsNavigationBlock(leaf)) {
        return getUrlsList(leaf.children);
      }
      if (isDocsNavigationExternalLink(leaf)) {
        return [leaf.url];
      }
      return [leaf];
    })
  );
}

export function getOpenChainFromPath(pathname: string, navTree: DocNavigationItem[]) {
  let chain: DocNavigationItem[] = [];

  let found = false;
  const walk = (leaf: DocNavigationItem, parents: DocNavigationItem[] = []) => {
    if (found) {
      return;
    }

    parents = parents.concat(leaf);

    if (isDocsNavigationBlock(leaf)) {
      leaf.children.forEach(child => {
        if (typeof child === 'string' && testPathAgainstUrl(child, pathname)) {
          chain = parents.concat(child);
          found = true;
        } else {
          walk(child, parents);
        }
      });
    } else if (typeof leaf === 'string' && testPathAgainstUrl(leaf, pathname)) {
      chain = parents;
      found = true;
    }
  };

  navTree.forEach(leaf => walk(leaf));

  return chain;
}

export function isDocsNavigationBlock(leaf?: DocNavigationItem): leaf is DocsNavigationBlock {
  return typeof leaf === 'object' && (leaf as DocsNavigationBlock).children !== undefined;
}

export function isDocsNavigationExternalLink(
  leaf?: DocNavigationItem
): leaf is DocsNavigationExternalLink {
  return typeof leaf === 'object' && (leaf as DocsNavigationExternalLink).url !== undefined;
}

export function testPathAgainstUrl(path: string, url: string) {
  const leadingRegEx = /^\//;
  const trailingRegEx = /\/$/;
  return (
    path.replace(leadingRegEx, '').replace(trailingRegEx, '') ===
    url.replace(leadingRegEx, '').replace(trailingRegEx, '')
  );
}
