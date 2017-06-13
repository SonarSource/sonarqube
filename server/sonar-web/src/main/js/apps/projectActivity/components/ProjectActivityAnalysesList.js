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
import { groupBy } from 'lodash';
import moment from 'moment';
import ProjectActivityAnalysis from './ProjectActivityAnalysis';
import FormattedDate from '../../../components/ui/FormattedDate';
import { translate } from '../../../helpers/l10n';
import type { Analysis } from '../types';

type Props = {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  addVersion: (analysis: string, version: string) => Promise<*>,
  analyses: Array<Analysis>,
  canAdmin: boolean,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteAnalysis: (analysis: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>
};

export default function ProjectActivityAnalysesList(props: Props) {
  if (props.analyses.length === 0) {
    return <div className="note">{translate('no_results')}</div>;
  }

  const firstAnalysis = props.analyses[0];

  const byDay = groupBy(props.analyses, analysis => moment(analysis.date).startOf('day').valueOf());

  return (
    <div className="boxed-group boxed-group-inner">
      <ul className="project-activity-days-list">
        {Object.keys(byDay).map(day => (
          <li
            key={day}
            className="project-activity-day"
            data-day={moment(Number(day)).format('YYYY-MM-DD')}>
            <div className="project-activity-date">
              <FormattedDate date={Number(day)} format="LL" />
            </div>

            <ul className="project-activity-analyses-list">
              {byDay[day] != null &&
                byDay[day].map(analysis => (
                  <ProjectActivityAnalysis
                    addCustomEvent={props.addCustomEvent}
                    addVersion={props.addVersion}
                    analysis={analysis}
                    canAdmin={props.canAdmin}
                    changeEvent={props.changeEvent}
                    deleteAnalysis={props.deleteAnalysis}
                    deleteEvent={props.deleteEvent}
                    isFirst={analysis === firstAnalysis}
                    key={analysis.key}
                  />
                ))}
            </ul>
          </li>
        ))}
      </ul>
    </div>
  );
}
