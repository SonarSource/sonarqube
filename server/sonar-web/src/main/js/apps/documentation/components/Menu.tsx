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
import OpenCloseIcon from '../../../components/icons-components/OpenCloseIcon';
import {
  activeOrChildrenActive,
  DocumentationEntry,
  getEntryChildren,
  getEntryRoot
} from '../utils';
import * as Docs from '../documentation.directory-loader';

interface Props {
  splat?: string;
}

export default class Menu extends React.PureComponent<Props> {
  getMenuEntriesHierarchy = (root?: string): Array<DocumentationEntry> => {
    const toplevelEntries = getEntryChildren(Docs as any, root);
    toplevelEntries.forEach(entry => {
      const entryRoot = getEntryRoot(entry.relativeName);
      entry.children = entryRoot !== '' ? this.getMenuEntriesHierarchy(entryRoot) : [];
    });
    return toplevelEntries;
  };

  renderEntry = (entry: DocumentationEntry, depth: number): React.ReactNode => {
    const active = entry.relativeName === this.props.splat;
    const opened = activeOrChildrenActive(this.props.splat || '', entry);
    const offset = 10 + 25 * depth;
    return (
      <React.Fragment key={entry.name}>
        <Link
          className={classNames('list-group-item', { active })}
          style={{ paddingLeft: offset }}
          to={'/documentation/' + entry.relativeName}>
          <h3 className="list-group-item-heading">
            {entry.children.length > 0 && (
              <OpenCloseIcon className="little-spacer-right" open={opened} />
            )}
            {entry.title}
          </h3>
        </Link>
        {opened && entry.children.map(entry => this.renderEntry(entry, depth + 1))}
      </React.Fragment>
    );
  };

  render() {
    return (
      <div className="api-documentation-results panel">
        <div className="list-group">
          {this.getMenuEntriesHierarchy().map(entry => this.renderEntry(entry, 0))}
        </div>
      </div>
    );
  }
}
