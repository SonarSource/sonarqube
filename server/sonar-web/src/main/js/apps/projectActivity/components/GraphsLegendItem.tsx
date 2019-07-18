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
import * as classNames from 'classnames';
import * as React from 'react';
import { ClearButton } from 'sonar-ui-common/components/controls/buttons';
import AlertWarnIcon from 'sonar-ui-common/components/icons/AlertWarnIcon';
import ChartLegendIcon from 'sonar-ui-common/components/icons/ChartLegendIcon';

interface Props {
  className?: string;
  index: number;
  metric: string;
  name: string;
  showWarning?: boolean;
  removeMetric?: (metric: string) => void;
}

export default class GraphsLegendItem extends React.PureComponent<Props> {
  handleClick = () => {
    if (this.props.removeMetric) {
      this.props.removeMetric(this.props.metric);
    }
  };

  render() {
    const isActionable = this.props.removeMetric != null;
    const legendClass = classNames(
      { 'project-activity-graph-legend-actionable': isActionable },
      this.props.className
    );
    return (
      <span className={legendClass}>
        {this.props.showWarning ? (
          <AlertWarnIcon className="spacer-right" />
        ) : (
          <ChartLegendIcon className="text-middle spacer-right" index={this.props.index} />
        )}
        <span className="text-middle">{this.props.name}</span>
        {isActionable && (
          <ClearButton
            className="button-tiny spacer-left text-middle"
            iconProps={{ size: 12 }}
            onClick={this.handleClick}
          />
        )}
      </span>
    );
  }
}
