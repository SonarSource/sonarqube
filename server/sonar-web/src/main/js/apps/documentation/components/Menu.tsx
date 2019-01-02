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
import MenuBlock from './MenuBlock';
import { MenuItem } from './MenuItem';
import { MenuExternalLink } from './MenuExternalLink';
import {
  DocumentationEntry,
  DocsNavigationBlock,
  getNodeFromUrl,
  isDocsNavigationBlock,
  isDocsNavigationExternalLink,
  DocsNavigationItem
} from '../utils';

interface Props {
  navigation: DocsNavigationItem[];
  pages: DocumentationEntry[];
  splat: string;
}

interface State {
  openBlockTitle: string;
}

export default class Menu extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      openBlockTitle: this.getOpenBlockFromLocation(this.props.splat)
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.splat !== nextProps.splat) {
      this.setState({ openBlockTitle: this.getOpenBlockFromLocation(nextProps.splat) });
    }
  }

  getOpenBlockFromLocation(splat: string) {
    const element = this.props.navigation.find(
      item => isDocsNavigationBlock(item) && item.children.some(child => '/' + splat === child)
    );
    return element ? (element as DocsNavigationBlock).title : '';
  }

  toggleBlock = (title: string) => {
    this.setState(state => ({ openBlockTitle: state.openBlockTitle === title ? '' : title }));
  };

  render() {
    return this.props.navigation.map(item => {
      if (isDocsNavigationBlock(item)) {
        return (
          <MenuBlock
            block={item}
            key={item.title}
            onToggle={this.toggleBlock}
            open={this.state.openBlockTitle === item.title}
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
          indent={false}
          key={item}
          node={getNodeFromUrl(this.props.pages, item)}
          splat={this.props.splat}
        />
      );
    });
  }
}
