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
import partition from 'lodash/partition';
import Events from './Events';
import FormattedDate from '../../../components/ui/FormattedDate';
import type { Analysis, Event } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';

export default class ProjectActivityAnalysis extends React.Component {
  props: {
    analysis: Analysis
  };

  render () {
    const { date, events } = this.props.analysis;
    const [versions, other] = partition(events, (event: Event) => event.category === 'VERSION');

    return (
        <li className="project-activity-analysis">
          <div className="project-activity-time">
            <FormattedDate date={date} format="LT" tooltipFormat="LTS"/>
          </div>

          <div className="project-activity-events">
            <div className="project-activity-events-primary">
              {translate('project_activity.project_analyzed')}
              {versions.length > 0 && (
                  <div className="spacer-top">
                    <Events events={versions}/>
                  </div>
              )}
            </div>

            {other.length > 0 && (
                <div className="project-activity-events-secondary">
                  <Events events={other}/>
                </div>
            )}
          </div>
        </li>
    );
  }
}
