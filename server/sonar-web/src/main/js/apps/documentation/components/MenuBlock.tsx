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
import * as classNames from 'classnames';
import { DocNavigationItem, DocsNavigationBlock } from 'Docs/@types/types';
import * as React from 'react';
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import OpenCloseIcon from 'sonar-ui-common/components/icons/OpenCloseIcon';
import { isDocsNavigationBlock } from '../navTreeUtils';
import { DocumentationEntry, getNodeFromUrl } from '../utils';
import { MenuItem } from './MenuItem';

interface Props {
  block: DocsNavigationBlock;
  depth?: number;
  openByDefault: boolean;
  openChain: DocNavigationItem[];
  pages: DocumentationEntry[];
  splat: string;
  title: string;
}

interface State {
  open: boolean;
}

export default class MenuBlock extends React.PureComponent<Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      open: props.openByDefault !== undefined ? props.openByDefault : false
    };
  }

  handleClick = () => {
    this.setState(prevState => ({
      open: !prevState.open
    }));
  };

  renderMenuItems = (block: DocsNavigationBlock): React.ReactNode => {
    const { depth = 0, openChain, pages, splat } = this.props;
    return block.children.map(item => {
      if (typeof item === 'string') {
        return (
          <MenuItem depth={depth + 1} key={item} node={getNodeFromUrl(pages, item)} splat={splat} />
        );
      } else if (isDocsNavigationBlock(item)) {
        return (
          <MenuBlock
            block={item}
            depth={depth + 1}
            key={item.title}
            openByDefault={openChain.includes(item)}
            openChain={openChain}
            pages={pages}
            splat={splat}
            title={item.title}
          />
        );
      } else {
        return null;
      }
    });
  };

  render() {
    const { block, depth = 0, title } = this.props;
    const { open } = this.state;
    const maxDepth = Math.min(depth, 3);
    return (
      <>
        <ButtonLink
          className={classNames('list-group-item', { [`depth-${maxDepth}`]: depth > 0 })}
          onClick={this.handleClick}>
          <h3 className="list-group-item-heading">
            <OpenCloseIcon className="little-spacer-right" open={open} />
            {title}
          </h3>
        </ButtonLink>
        {open && this.renderMenuItems(block)}
      </>
    );
  }
}
