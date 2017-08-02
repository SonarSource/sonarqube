/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import classNames from 'classnames';
import QualifierIcon from '../../../../components/shared/QualifierIcon';
import { splitPath } from '../../../../helpers/path';
import { getComponentUrl } from '../../../../helpers/urls';
import type { Component } from '../../types';

type Props = {
  component: Component,
  isSelected: boolean,
  onClick: Component => void
};

export default class ComponentCell extends React.PureComponent {
  props: Props;

  handleClick = (e: MouseEvent) => {
    const isLeftClickEvent = e.button === 0;
    const isModifiedEvent = !!(e.metaKey || e.altKey || e.ctrlKey || e.shiftKey);

    if (isLeftClickEvent && !isModifiedEvent) {
      e.preventDefault();
      this.props.onClick();
    }
  };

  renderInner() {
    const { component } = this.props;
    let head = '';
    let tail = component.name;

    if (['DIR', 'FIL', 'UTS'].includes(component.qualifier)) {
      const parts = splitPath(component.path);
      head = parts.head;
      tail = parts.tail;
    }
    return (
      <span title={component.refKey || component.key}>
        <QualifierIcon qualifier={component.qualifier} />
        &nbsp;
        {head.length > 0 &&
          <span className="note">
            {head}/
          </span>}
        <span>{tail}</span>
      </span>
    );
  }

  render() {
    const { component } = this.props;
    const linkClassName = classNames('link-no-underline', {
      selected: this.props.isSelected
    });

    return (
      <td className="measure-details-component-cell">
        <div className="text-ellipsis">
          {component.refId == null
            ? <a
                id={'component-measures-component-link-' + component.key}
                className={linkClassName}
                href={getComponentUrl(component.key)}
                onClick={this.handleClick}>
                {this.renderInner()}
              </a>
            : <a
                id={'component-measures-component-link-' + component.key}
                className={linkClassName}
                href={getComponentUrl(component.refKey || component.key)}>
                <span className="big-spacer-right">
                  <i className="icon-detach" />
                </span>
                {this.renderInner()}
              </a>}
        </div>
      </td>
    );
  }
}
