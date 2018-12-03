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
import * as key from 'keymaster';
import Breadcrumb from './Breadcrumb';
import { getBreadcrumbs } from '../../../api/components';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';

interface Props {
  backToFirst: boolean;
  branchLike?: T.BranchLike;
  className?: string;
  component: T.ComponentMeasure;
  handleSelect: (component: string) => void;
  rootComponent: T.ComponentMeasure;
}

interface State {
  breadcrumbs: T.ComponentMeasure[];
}

export default class Breadcrumbs extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { breadcrumbs: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchBreadcrumbs();
    this.attachShortcuts();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.component !== prevProps.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike)
    ) {
      this.fetchBreadcrumbs();
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

  fetchBreadcrumbs = () => {
    const { branchLike, component, rootComponent } = this.props;
    const isRoot = component.key === rootComponent.key;
    if (isRoot) {
      if (this.mounted) {
        this.setState({ breadcrumbs: [component] });
      }
      return;
    }
    getBreadcrumbs({ component: component.key, ...getBranchLikeQuery(branchLike) }).then(
      breadcrumbs => {
        if (this.mounted) {
          this.setState({ breadcrumbs });
        }
      },
      () => {}
    );
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
            canBrowse={component.key !== lastItem.key}
            component={component}
            handleSelect={this.props.handleSelect}
            isLast={component.key === lastItem.key}
            key={component.key}
          />
        ))}
      </div>
    );
  }
}
