/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import classNames from 'classnames';

export default class Filter extends React.Component {
  static propTypes = {
    getFilterUrl: React.PropTypes.func.isRequired,
    isOpen: React.PropTypes.bool.isRequired,
    renderName: React.PropTypes.func.isRequired,
    renderOptions: React.PropTypes.func.isRequired,
    toggleFilter: React.PropTypes.func.isRequired,
    value: React.PropTypes.any
  };

  handleClick (e) {
    e.preventDefault();
    e.target.blur();
    this.props.toggleFilter();
  }

  render () {
    const { value, isOpen } = this.props;
    const { renderName, renderOptions } = this.props;
    const hasValue = value != null;
    const className = classNames('search-navigator-facet-box', {
      'search-navigator-facet-box-collapsed': !hasValue && !isOpen
    });
    return (
        <div className={className}>
          <a className="search-navigator-facet-header projects-facet-header"
             href="#"
             onClick={e => this.handleClick(e)}>
            <i className={'icon-checkbox ' + ((hasValue || isOpen) ? 'icon-checkbox-checked' : '')}/>
            {' '}
            {renderName()}
          </a>

          {(hasValue || isOpen) && (
              <div className="search-navigator-facet-list">
                {renderOptions()}
              </div>
          )}
        </div>
    );
  }
}
