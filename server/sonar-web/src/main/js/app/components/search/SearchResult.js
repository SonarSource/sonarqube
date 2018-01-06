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
import { Link } from 'react-router';
/*:: import type { Component } from './utils'; */
import FavoriteIcon from '../../../components/icons-components/FavoriteIcon';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import ClockIcon from '../../../components/common/ClockIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { getProjectUrl } from '../../../helpers/urls';

/*::
type Props = {|
  appState: { organizationsEnabled: boolean },
  component: Component,
  innerRef: (string, HTMLElement) => void,
  onClose: () => void,
  onSelect: string => void,
  organizations: { [string]: { name: string } },
  projects: { [string]: { name: string } },
  selected: boolean
|};
*/

/*::
type State = {
  tooltipVisible: boolean
};
*/

const TOOLTIP_DELAY = 1000;

export default class SearchResult extends React.PureComponent {
  /*:: interval: ?number; */
  /*:: props: Props; */
  state /*: State */ = { tooltipVisible: false };

  componentDidMount() {
    if (this.props.selected) {
      this.scheduleTooltip();
    }
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (!this.props.selected && nextProps.selected) {
      this.scheduleTooltip();
    } else if (this.props.selected && !nextProps.selected) {
      this.unscheduleTooltip();
      this.setState({ tooltipVisible: false });
    }
  }

  componentWillUnmount() {
    this.unscheduleTooltip();
  }

  scheduleTooltip = () => {
    this.interval = setTimeout(() => this.setState({ tooltipVisible: true }), TOOLTIP_DELAY);
  };

  unscheduleTooltip = () => {
    if (this.interval) {
      clearInterval(this.interval);
    }
  };

  handleMouseEnter = () => {
    this.props.onSelect(this.props.component.key);
  };

  renderOrganization = (component /*: Component */) => {
    if (!this.props.appState.organizationsEnabled) {
      return null;
    }

    if (
      !['VW', 'SVW', 'APP', 'TRK'].includes(component.qualifier) ||
      component.organization == null
    ) {
      return null;
    }

    const organization = this.props.organizations[component.organization];
    return organization ? (
      <div className="navbar-search-item-right text-muted-2">{organization.name}</div>
    ) : null;
  };

  renderProject = (component /*: Component */) => {
    if (!['BRC', 'FIL', 'UTS'].includes(component.qualifier) || component.project == null) {
      return null;
    }

    const project = this.props.projects[component.project];
    return project ? (
      <div className="navbar-search-item-right text-muted-2">{project.name}</div>
    ) : null;
  };

  render() {
    const { component } = this.props;

    return (
      <li
        className={this.props.selected ? 'active' : undefined}
        key={component.key}
        ref={node => this.props.innerRef(component.key, node)}>
        <Tooltip
          mouseEnterDelay={TOOLTIP_DELAY / 1000}
          overlay={component.key}
          placement="left"
          visible={this.state.tooltipVisible}>
          <Link
            className="navbar-search-item-link"
            data-key={component.key}
            onClick={this.props.onClose}
            onMouseEnter={this.handleMouseEnter}
            to={getProjectUrl(component.key)}>
            <span className="navbar-search-item-icons little-spacer-right">
              {component.isFavorite && <FavoriteIcon favorite={true} size={12} />}
              {!component.isFavorite && component.isRecentlyBrowsed && <ClockIcon size={12} />}
              <QualifierIcon className="little-spacer-right" qualifier={component.qualifier} />
            </span>

            {component.match ? (
              <span
                className="navbar-search-item-match"
                dangerouslySetInnerHTML={{ __html: component.match }}
              />
            ) : (
              <span className="navbar-search-item-match">{component.name}</span>
            )}

            {this.renderOrganization(component)}
            {this.renderProject(component)}
          </Link>
        </Tooltip>
      </li>
    );
  }
}
