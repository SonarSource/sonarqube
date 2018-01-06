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
import classNames from 'classnames';
import Events from './Events';
import AddEventForm from './forms/AddEventForm';
import RemoveAnalysisForm from './forms/RemoveAnalysisForm';
import TimeTooltipFormatter from '../../../components/intl/TimeTooltipFormatter';
import ActionsDropdown, {
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import { translate } from '../../../helpers/l10n';
/*:: import type { Analysis } from '../types'; */

/*::
type Props = {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<*>,
  addVersion: (analysis: string, version: string) => Promise<*>,
  analysis: Analysis,
  canAdmin: boolean,
  canDeleteAnalyses: boolean,
  canCreateVersion: boolean,
  changeEvent: (event: string, name: string) => Promise<*>,
  deleteAnalysis: (analysis: string) => Promise<*>,
  deleteEvent: (analysis: string, event: string) => Promise<*>,
  isFirst: boolean,
  selected: boolean,
  updateSelectedDate: Date => void
};
*/

export default class ProjectActivityAnalysis extends React.PureComponent {
  /*:: props: Props; */

  handleClick = () => this.props.updateSelectedDate(this.props.analysis.date);

  stopPropagation = (e /*: Event */) => e.stopPropagation();

  render() {
    const { analysis, isFirst, canAdmin } = this.props;
    const { date, events } = analysis;
    const analysisTitle = translate('project_activity.analysis');
    const hasVersion = events.find(event => event.category === 'VERSION') != null;

    const canAddVersion = canAdmin && !hasVersion && this.props.canCreateVersion;
    const canAddEvent = canAdmin;
    const canDeleteAnalyses = this.props.canDeleteAnalyses && !isFirst;

    return (
      <li
        className={classNames('project-activity-analysis clearfix', {
          selected: this.props.selected
        })}
        data-date={date.valueOf()}
        onClick={this.handleClick}
        role="listitem"
        tabIndex="0">
        <div className="project-activity-time spacer-right">
          <TimeTooltipFormatter className="text-middle" date={date} placement="right" />
        </div>
        <div className="project-activity-analysis-icon spacer-right" title={analysisTitle} />

        {(canAddVersion || canAddEvent || canDeleteAnalyses) && (
          <div className="project-activity-analysis-actions big-spacer-right">
            <ActionsDropdown menuPosition="left" small={true} toggleClassName="js-analysis-actions">
              {canAddVersion && (
                <AddEventForm
                  addEvent={this.props.addVersion}
                  analysis={analysis}
                  addEventButtonText="project_activity.add_version"
                />
              )}
              {canAddEvent && (
                <AddEventForm
                  addEvent={this.props.addCustomEvent}
                  analysis={analysis}
                  addEventButtonText="project_activity.add_custom_event"
                />
              )}
              {(canAddVersion || canAddEvent) && canDeleteAnalyses && <ActionsDropdownDivider />}
              {canDeleteAnalyses && (
                <RemoveAnalysisForm
                  analysis={analysis}
                  deleteAnalysis={this.props.deleteAnalysis}
                />
              )}
            </ActionsDropdown>
          </div>
        )}

        {events.length > 0 && (
          <Events
            analysis={analysis.key}
            canAdmin={canAdmin}
            canDeleteAnalyses={this.props.canDeleteAnalyses}
            changeEvent={this.props.changeEvent}
            deleteEvent={this.props.deleteEvent}
            events={events}
            isFirst={this.props.isFirst}
          />
        )}
      </li>
    );
  }
}
