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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import { sortBy } from 'lodash';
import {
  getEntryChildren,
  DocumentationEntry,
  activeOrChildrenActive,
  getEntryRoot
} from '../utils';
import OpenCloseIcon from '../../../components/icons-components/OpenCloseIcon';

interface Props {
  pages: DocumentationEntry[];
  splat: string;
}

type EntryWithChildren = DocumentationEntry & { children?: DocumentationEntry[] };

export default class Menu extends React.PureComponent<Props> {
  getMenuEntriesHierarchy = (root?: string): EntryWithChildren[] => {
    const topLevelEntries = getEntryChildren(this.props.pages, root);
    return sortBy(
      topLevelEntries.map(entry => {
        const entryRoot = getEntryRoot(entry.relativeName);
        const children = entryRoot !== '' ? this.getMenuEntriesHierarchy(entryRoot) : [];
        return { ...entry, children };
      }),
      entry => entry.order
    );
  };

  renderEntry = (entry: EntryWithChildren, depth: number): React.ReactNode => {
    const active = entry.relativeName === this.props.splat;
    const opened = activeOrChildrenActive(this.props.splat || '', entry);
    const offset = 10 + 25 * depth;
    const { children = [] } = entry;
    return (
      <React.Fragment key={entry.relativeName}>
        <Link
          className={classNames('list-group-item', { active })}
          style={{ paddingLeft: offset }}
          to={'/documentation/' + entry.relativeName}>
          <h3 className="list-group-item-heading">
            {children.length > 0 && <OpenCloseIcon className="little-spacer-right" open={opened} />}
            {entry.title}
          </h3>
        </Link>
        {opened && children.map(entry => this.renderEntry(entry, depth + 1))}
      </React.Fragment>
    );
  };

  render() {
    return <>{this.getMenuEntriesHierarchy().map(entry => this.renderEntry(entry, 0))}</>;
  }
}
