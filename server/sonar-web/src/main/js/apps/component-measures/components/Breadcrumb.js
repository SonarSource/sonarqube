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
import Tooltip from '../../../components/controls/Tooltip';
import { collapsePath, limitComponentName } from '../../../helpers/path';
/*:: import type { Component } from '../types'; */

/*:: type Props = {
  canBrowse: boolean,
  component: Component,
  isLast: boolean,
  handleSelect: string => void
}; */

export default class Breadcrumb extends React.PureComponent {
  /*:: props: Props; */

  handleClick = (e /*: Event & { target: HTMLElement } */) => {
    e.preventDefault();
    e.target.blur();
    this.props.handleSelect(this.props.component.key);
  };

  render() {
    const { canBrowse, component, isLast } = this.props;
    const isPath = component.qualifier === 'DIR';
    const componentName = isPath
      ? collapsePath(component.name, 15)
      : limitComponentName(component.name);
    const breadcrumbItem = canBrowse ? (
      <a href="#" onClick={this.handleClick}>
        {componentName}
      </a>
    ) : (
      <span>{componentName}</span>
    );

    return (
      <span>
        {component.name !== componentName ? (
          <Tooltip overlay={component.name} placement="bottom">
            {breadcrumbItem}
          </Tooltip>
        ) : (
          breadcrumbItem
        )}
        {!isLast && <span className="slash-separator" />}
      </span>
    );
  }
}
