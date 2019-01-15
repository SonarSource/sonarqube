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
import classNames from 'classnames';
import PageLink from './PageLink';
import ChevronDownIcon from './icons/ChevronDownIcon';
import ChevronUpIcon from './icons/ChevronUpIcon';
import { MarkdownRemark } from '../@types/graphql-types';

interface Props {
  children: MarkdownRemark[];
  location: Location;
  onToggle: (title: string) => void;
  open: boolean;
  title: string;
}

export default class CategoryLink extends React.PureComponent<Props> {
  handleToggle = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.props.onToggle(this.props.title);
  };

  render() {
    const { children, location, title, open } = this.props;
    return (
      <div>
        <a
          className={classNames('page-indexes-link', { active: open })}
          href="#"
          onClick={this.handleToggle}>
          {open ? <ChevronUpIcon /> : <ChevronDownIcon />}
          {title}
        </a>
        {children && open && (
          <div className="sub-menu">
            {children.map(page => (
              <PageLink className="sub-menu-link" key={page.id} location={location} node={page} />
            ))}
          </div>
        )}
      </div>
    );
  }
}
