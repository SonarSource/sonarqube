/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';

export interface Props {
  active?: boolean;
  className?: string;
  halfWidth?: boolean;
  name: React.ReactNode;
  onClick: (x: string, multiple?: boolean) => void;
  stat?: React.ReactNode;
  /** Textual version of `name` */
  tooltip?: string;
  value: string;
}

export default class FacetItem extends React.PureComponent<Props> {
  static defaultProps = {
    halfWidth: false,
    loading: false,
  };

  handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    this.props.onClick(this.props.value, event.ctrlKey || event.metaKey);
  };

  renderValue() {
    if (this.props.stat == null) {
      return null;
    }

    return <span className="facet-stat">{this.props.stat}</span>;
  }

  render() {
    const { name, halfWidth, active, value, tooltip } = this.props;
    const className = classNames('search-navigator-facet button-link', this.props.className, {
      active,
    });

    return (
      <span role="listitem" className={classNames({ 'search-navigator-facet-half': halfWidth })}>
        <button
          aria-checked={active}
          className={className}
          data-facet={value}
          onClick={this.handleClick}
          tabIndex={0}
          title={tooltip}
          role="checkbox"
          type="button"
        >
          <span className="facet-name">{name}</span>
          {this.renderValue()}
        </button>
      </span>
    );
  }
}
