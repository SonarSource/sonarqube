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
import { DocNavigationItem } from 'Docs/@types/types';
import * as React from 'react';
import {
  getOpenChainFromPath,
  isDocsNavigationBlock,
  isDocsNavigationExternalLink
} from '../navTreeUtils';
import { DocumentationEntry, getNodeFromUrl } from '../utils';
import MenuBlock from './MenuBlock';
import { MenuExternalLink } from './MenuExternalLink';
import { MenuItem } from './MenuItem';

interface Props {
  navigation: DocNavigationItem[];
  pages: DocumentationEntry[];
  splat: string;
}

interface State {
  openChain: DocNavigationItem[];
}

export default class Menu extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      openChain: getOpenChainFromPath(this.props.splat, this.props.navigation)
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.splat !== nextProps.splat) {
      this.setState({ openChain: getOpenChainFromPath(nextProps.splat, nextProps.navigation) });
    }
  }

  render() {
    const { openChain } = this.state;
    return (
      <>
        {this.props.navigation.map(item => {
          if (isDocsNavigationBlock(item)) {
            return (
              <MenuBlock
                block={item}
                key={item.title}
                openByDefault={openChain.includes(item)}
                openChain={openChain}
                pages={this.props.pages}
                splat={this.props.splat}
                title={item.title}
              />
            );
          }
          if (isDocsNavigationExternalLink(item)) {
            return <MenuExternalLink key={item.title} title={item.title} url={item.url} />;
          }
          return (
            <MenuItem
              key={item}
              node={getNodeFromUrl(this.props.pages, item)}
              splat={this.props.splat}
            />
          );
        })}
      </>
    );
  }
}
