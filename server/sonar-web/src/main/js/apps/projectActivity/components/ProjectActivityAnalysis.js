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
import AddEventForm from './forms/AddEventForm';
import RemoveAnalysisForm from './forms/RemoveAnalysisForm';
import FormattedDate from '../../../components/ui/FormattedDate';
import { translate } from '../../../helpers/l10n';
import type { Analysis } from '../types';

type Props = {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  addVersion: (analysis: string, version: string) => Promise<*>,
  analysis: Analysis,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteAnalysis: (analysis: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  isFirst: boolean,
  canAdmin: boolean
};

export default function ProjectActivityAnalysis(props: Props) {
  const { date, events } = props.analysis;
  const { isFirst, canAdmin } = props;

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
                  <AddEventForm
                    addEvent={props.addVersion}
                    analysis={props.analysis}
                    addEventButtonText="project_activity.add_version"
                  />
                </li>}
              <li>
                <AddEventForm
                  addEvent={props.addCustomEvent}
                  analysis={props.analysis}
                  addEventButtonText="project_activity.add_custom_event"
                />
              </li>
            </ul>
          </div>

          {!isFirst &&
            <div className="display-inline-block little-spacer-left">
              <RemoveAnalysisForm analysis={props.analysis} deleteAnalysis={props.deleteAnalysis} />
            </div>}
        </div>}

      <div className="project-activity-time">
        <FormattedDate date={date} format="LT" tooltipFormat="LTS" />
      </div>

      {events.length > 0 &&
        <Events
          analysis={props.analysis.key}
          canAdmin={canAdmin}
          changeEvent={props.changeEvent}
          deleteEvent={props.deleteEvent}
          events={events}
          isFirst={props.isFirst}
        />}
    </li>
  );
}
