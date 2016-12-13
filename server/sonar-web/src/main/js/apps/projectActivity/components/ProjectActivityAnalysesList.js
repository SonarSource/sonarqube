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
import { connect } from 'react-redux';
import groupBy from 'lodash/groupBy';
import moment from 'moment';
import ProjectActivityAnalysis from './ProjectActivityAnalysis';
import FormattedDate from '../../../components/ui/FormattedDate';
import { getProjectActivity } from '../../../store/rootReducer';
import { getAnalyses } from '../../../store/projectActivity/duck';
import { translate } from '../../../helpers/l10n';

class ProjectActivityAnalysesList extends React.Component {
  props: {
    project: string,
    analyses?: Array<{
      key: string,
      date: string
    }>
  };

  render () {
    if (!this.props.analyses) {
      return null;
    }

    if (this.props.analyses.length === 0) {
      return (
          <div className="note">{translate('no_results')}</div>
      );
    }

    const byDay = groupBy(this.props.analyses, analysis => moment(analysis.date).startOf('day').valueOf());

    return (
        <ul className="project-activity-days-list">
          {Object.keys(byDay).map(day => (
              <li key={day} className="project-activity-day">
                <div className="project-activity-date">
                  <FormattedDate date={Number(day)} format="LL"/>
                </div>

                <ul className="project-activity-analyses-list">
                  {byDay[day].map(analysis => (
                      <ProjectActivityAnalysis key={analysis.key} analysis={analysis}/>
                  ))}
                </ul>
              </li>
          ))}
        </ul>
    );
  }
}

const mapStateToProps = (state, ownProps) => ({
  analyses: getAnalyses(getProjectActivity(state), ownProps.project)
});

export default connect(mapStateToProps)(ProjectActivityAnalysesList);
