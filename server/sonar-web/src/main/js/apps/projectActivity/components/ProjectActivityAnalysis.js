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
import Events from './Events';
import AddVersionForm from './forms/AddVersionForm';
import AddCustomEventForm from './forms/AddCustomEventForm';
import RemoveAnalysisForm from './forms/RemoveAnalysisForm';
import FormattedDate from '../../../components/ui/FormattedDate';
import type { Analysis } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';

export default class ProjectActivityAnalysis extends React.Component {
  props: {
    analysis: Analysis,
    isFirst: boolean,
    project: string,
    canAdmin: boolean
  };

  render() {
    const { date, events } = this.props.analysis;
    const { isFirst, canAdmin } = this.props;

    const version = events.find(event => event.category === 'VERSION');

    return (
      <li className="project-activity-analysis clearfix">
        {canAdmin &&
          <div className="project-activity-analysis-actions">
            <div className="dropdown display-inline-block">
              <button className="js-create button-small" data-toggle="dropdown">
                {translate('create')} <i className="icon-dropdown" />
              </button>
              <ul className="dropdown-menu dropdown-menu-right">
                {version == null &&
                  <li>
                    <AddVersionForm analysis={this.props.analysis} />
                  </li>}
                <li>
                  <AddCustomEventForm analysis={this.props.analysis} />
                </li>
              </ul>
            </div>

            {!isFirst &&
              <div className="display-inline-block little-spacer-left">
                <RemoveAnalysisForm analysis={this.props.analysis} project={this.props.project} />
              </div>}
          </div>}

        <div className="project-activity-time">
          <FormattedDate date={date} format="LT" tooltipFormat="LTS" />
        </div>

        {events.length > 0 &&
          <Events
            analysis={this.props.analysis.key}
            events={events}
            isFirst={this.props.isFirst}
            canAdmin={canAdmin}
          />}
      </li>
    );
  }
}
