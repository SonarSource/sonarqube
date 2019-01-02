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
import { MenuItem } from './MenuItem';
import { DocumentationEntry, DocsNavigationBlock, getNodeFromUrl } from '../utils';
import OpenCloseIcon from '../../../components/icons-components/OpenCloseIcon';

interface Props {
  block: DocsNavigationBlock;
  onToggle: (title: string) => void;
  open: boolean;
  pages: DocumentationEntry[];
  splat: string;
  title: string;
}

export default class MenuBlock extends React.PureComponent<Props> {
  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.stopPropagation();
    event.preventDefault();
    this.props.onToggle(this.props.title);
  };

  render() {
    const { open, block, pages, title, splat } = this.props;
    return (
      <>
        <a className="list-group-item" href="#" onClick={this.handleClick}>
          <h3 className="list-group-item-heading">
            <OpenCloseIcon className="little-spacer-right" open={this.props.open} />
            {title}
          </h3>
        </a>
        {open &&
          block.children.map(item => (
            <MenuItem indent={true} key={item} node={getNodeFromUrl(pages, item)} splat={splat} />
          ))}
      </>
    );
  }
}
