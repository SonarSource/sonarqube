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
import { Link } from 'react-router';
import { ComponentResult } from './utils';
import FavoriteIcon from '../../../components/icons-components/FavoriteIcon';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import ClockIcon from '../../../components/icons-components/ClockIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { getProjectUrl, getCodeUrl } from '../../../helpers/urls';

interface Props {
  appState: Pick<T.AppState, 'organizationsEnabled'>;
  component: ComponentResult;
  innerRef: (componentKey: string, node: HTMLElement | null) => void;
  onClose: () => void;
  onSelect: (componentKey: string) => void;
  organizations: T.Dict<{ name: string }>;
  projects: T.Dict<{ name: string }>;
  selected: boolean;
}

interface State {
  tooltipVisible: boolean;
}

const TOOLTIP_DELAY = 1000;

export default class SearchResult extends React.PureComponent<Props, State> {
  interval?: number;
  state: State = { tooltipVisible: false };

  componentDidMount() {
    if (this.props.selected) {
      this.scheduleTooltip();
    }
  }

  componentWillReceiveProps(nextProps: Props) {
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
    this.interval = window.setTimeout(() => {
      this.setState({ tooltipVisible: true });
    }, TOOLTIP_DELAY);
  };

  unscheduleTooltip = () => {
    if (this.interval) {
      window.clearInterval(this.interval);
    }
  };

  handleMouseEnter = () => {
    this.props.onSelect(this.props.component.key);
  };

  renderOrganization = (component: ComponentResult) => {
    if (!this.props.appState.organizationsEnabled) {
      return null;
    }

    if (!['VW', 'SVW', 'APP', 'TRK'].includes(component.qualifier) || !component.organization) {
      return null;
    }

    const organization = this.props.organizations[component.organization];
    return organization ? (
      <div className="navbar-search-item-right text-muted-2">{organization.name}</div>
    ) : null;
  };

  renderProject = (component: ComponentResult) => {
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

    const isFile = component.qualifier === 'FIL' || component.qualifier === 'UTS';
    const to = isFile
      ? getCodeUrl(component.project!, undefined, component.key)
      : getProjectUrl(component.key);

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
          <Link data-key={component.key} onClick={this.props.onClose} to={to}>
            <span className="navbar-search-item-link" onMouseEnter={this.handleMouseEnter}>
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
            </span>
          </Link>
        </Tooltip>
      </li>
    );
  }
}
