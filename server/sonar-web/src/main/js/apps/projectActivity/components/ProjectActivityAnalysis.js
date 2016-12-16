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
// @flow
import React from 'react';
import Events from './Events';
import AddVersionForm from './forms/AddVersionForm';
import AddCustomEventForm from './forms/AddCustomEventForm';
import FormattedDate from '../../../components/ui/FormattedDate';
import type { Analysis } from '../../../store/projectActivity/duck';

export default class ProjectActivityAnalysis extends React.Component {
  props: {
    analysis: Analysis,
    isFirst: boolean,
    project: string,
    canAdmin: boolean
  };

  render () {
    const { date, events } = this.props.analysis;
    const { canAdmin } = this.props;

    const version = events.find(event => event.category === 'VERSION');

    return (
        <li className="project-activity-analysis clearfix">
          {canAdmin && (
              <div className="project-activity-analysis-actions">
                {version == null && (
                    <AddVersionForm
                        analysis={this.props.analysis}
                        project={this.props.project}/>
                )}

                <AddCustomEventForm
                    analysis={this.props.analysis}
                    project={this.props.project}/>
              </div>
          )}

          <div className="project-activity-time">
            <FormattedDate date={date} format="LT" tooltipFormat="LTS"/>
          </div>

          <div className="project-activity-events">
            {events.length > 0 && (
                <Events
                    analysis={this.props.analysis.key}
                    events={events}
                    project={this.props.project}
                    canAdmin={canAdmin}/>
            )}
          </div>
        </li>
    );
  }
}
