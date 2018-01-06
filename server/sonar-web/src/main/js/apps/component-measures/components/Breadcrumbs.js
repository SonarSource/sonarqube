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
// @flow
import React from 'react';
import key from 'keymaster';
import Breadcrumb from './Breadcrumb';
import { getBreadcrumbs } from '../../../api/components';
/*:: import type { Component } from '../types'; */

/*:: type Props = {|
  backToFirst: boolean,
  branch?: string,
  className?: string,
  component: Component,
  handleSelect: string => void,
  rootComponent: Component
|}; */

/*:: type State = {
  breadcrumbs: Array<Component>
}; */

export default class Breadcrumbs extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    breadcrumbs: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchBreadcrumbs(this.props);
    this.attachShortcuts();
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (this.props.component !== nextProps.component) {
      this.fetchBreadcrumbs(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    this.detachShortcuts();
  }

  attachShortcuts() {
    key('left', 'measures-files', () => {
      const { breadcrumbs } = this.state;
      if (breadcrumbs.length > 1) {
        const idx = this.props.backToFirst ? 0 : breadcrumbs.length - 2;
        this.props.handleSelect(breadcrumbs[idx].key);
      }
      return false;
    });
  }

  detachShortcuts() {
    key.unbind('left', 'measures-files');
  }

  fetchBreadcrumbs = ({ branch, component, rootComponent } /*: Props */) => {
    const isRoot = component.key === rootComponent.key;
    if (isRoot) {
      if (this.mounted) {
        this.setState({ breadcrumbs: [component] });
      }
      return;
    }
    getBreadcrumbs(component.key, branch).then(breadcrumbs => {
      if (this.mounted) {
        this.setState({ breadcrumbs });
      }
    });
  };

  render() {
    const { breadcrumbs } = this.state;
    if (breadcrumbs.length <= 0) {
      return null;
    }
    const lastItem = breadcrumbs[breadcrumbs.length - 1];
    return (
      <div className={this.props.className}>
        {breadcrumbs.map(component => (
          <Breadcrumb
            key={component.key}
            canBrowse={component.key !== lastItem.key}
            component={component}
            isLast={component.key === lastItem.key}
            handleSelect={this.props.handleSelect}
          />
        ))}
      </div>
    );
  }
}
