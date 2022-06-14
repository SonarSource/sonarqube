/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import Tooltip from '../../../components/controls/Tooltip';
import ClockIcon from '../../../components/icons/ClockIcon';
import FavoriteIcon from '../../../components/icons/FavoriteIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { getComponentOverviewUrl } from '../../../helpers/urls';
import { Dict } from '../../../types/types';
import { ComponentResult } from './utils';

interface Props {
  component: ComponentResult;
  innerRef: (componentKey: string, node: HTMLElement | null) => void;
  onClose: () => void;
  onSelect: (componentKey: string) => void;
  projects: Dict<{ name: string }>;
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

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.selected && this.props.selected) {
      this.scheduleTooltip();
    } else if (prevProps.selected && !this.props.selected) {
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

  renderProject = (component: ComponentResult) => {
    if (component.project == null) {
      return null;
    }

    const project = this.props.projects[component.project];
    return project ? (
      <div className="navbar-search-item-right text-muted-2">{project.name}</div>
    ) : null;
  };

  render() {
    const { component } = this.props;

    const to = getComponentOverviewUrl(component.key, component.qualifier);

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
                  // Safe: comes from the backend
                  dangerouslySetInnerHTML={{ __html: component.match }}
                />
              ) : (
                <span className="navbar-search-item-match">{component.name}</span>
              )}

              {this.renderProject(component)}
            </span>
          </Link>
        </Tooltip>
      </li>
    );
  }
}
