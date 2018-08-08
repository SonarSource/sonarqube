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
import * as classNames from 'classnames';

export interface Props {
  active?: boolean;
  className?: string;
  disabled?: boolean;
  halfWidth?: boolean;
  loading?: boolean;
  name: React.ReactNode;
  onClick: (x: string, multiple?: boolean) => void;
  stat?: React.ReactNode;
  /** Textual version of `name` */
  tooltip: string;
  value: string;
}

export default class FacetItem extends React.PureComponent<Props> {
  static defaultProps = {
    disabled: false,
    halfWidth: false,
    loading: false
  };

  handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClick(this.props.value, event.ctrlKey || event.metaKey);
  };

  render() {
    const { name } = this.props;
    const className = classNames('search-navigator-facet', this.props.className, {
      active: this.props.active,
      'search-navigator-facet-half': this.props.halfWidth
    });

    return this.props.disabled ? (
      <span className={className} data-facet={this.props.value} title={this.props.tooltip}>
        <span className="facet-name">{name}</span>
        {this.props.stat != null && <span className="facet-stat">{this.props.stat}</span>}
      </span>
    ) : (
      <a
        className={className}
        data-facet={this.props.value}
        href="#"
        onClick={this.handleClick}
        title={this.props.tooltip}>
        <span className="facet-name">{name}</span>
        {this.props.stat != null && <span className="facet-stat">{this.props.stat}</span>}
      </a>
    );
  }
}
