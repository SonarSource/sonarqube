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
  ActionsDropdownDivider,
  ActionsDropdownItem
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

type State = {
  addEventForm: bool,
  addVersionForm: bool,
  removeAnalysisForm: bool
}
*/

export default class ProjectActivityAnalysis extends React.PureComponent {
  mounted /*: boolean */ = false;
  /*:: props: Props; */
  state /*: State */ = { addEventForm: false, addVersionForm: false, removeAnalysisForm: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = () => this.props.updateSelectedDate(this.props.analysis.date);

  stopPropagation = (e /*: Event */) => e.stopPropagation();

  handleRemoveAnalysisClick = () => {
    this.setState({ removeAnalysisForm: true });
  };

  closeRemoveAnalysisForm = () => {
    if (this.mounted) {
      this.setState({ removeAnalysisForm: false });
    }
  };

  handleAddEventClick = () => {
    this.setState({ addEventForm: true });
  };

  closeAddEventForm = () => {
    if (this.mounted) {
      this.setState({ addEventForm: false });
    }
  };

  handleAddVersionClick = () => {
    this.setState({ addVersionForm: true });
  };

  closeAddVersionForm = () => {
    if (this.mounted) {
      this.setState({ addVersionForm: false });
    }
  };

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
        tabIndex="0">
        <div className="project-activity-time spacer-right">
          <TimeTooltipFormatter className="text-middle" date={date} />
        </div>
        <div className="project-activity-analysis-icon spacer-right" title={analysisTitle} />

        {(canAddVersion || canAddEvent || canDeleteAnalyses) && (
          <div className="project-activity-analysis-actions big-spacer-right">
            <ActionsDropdown small={true} toggleClassName="js-analysis-actions">
              {canAddVersion && (
                <ActionsDropdownItem className="js-add-event" onClick={this.handleAddVersionClick}>
                  {translate('project_activity.add_version')}
                </ActionsDropdownItem>
              )}
              {canAddEvent && (
                <ActionsDropdownItem className="js-add-event" onClick={this.handleAddEventClick}>
                  {translate('project_activity.add_custom_event')}
                </ActionsDropdownItem>
              )}
              {(canAddVersion || canAddEvent) && canDeleteAnalyses && <ActionsDropdownDivider />}
              {canDeleteAnalyses && (
                <ActionsDropdownItem
                  className="js-delete-analysis"
                  destructive={true}
                  onClick={this.handleRemoveAnalysisClick}>
                  {translate('project_activity.delete_analysis')}
                </ActionsDropdownItem>
              )}
            </ActionsDropdown>

            {this.state.addVersionForm && (
              <AddEventForm
                addEvent={this.props.addVersion}
                addEventButtonText="project_activity.add_version"
                analysis={analysis}
                onClose={this.closeAddVersionForm}
              />
            )}

            {this.state.addEventForm && (
              <AddEventForm
                addEvent={this.props.addCustomEvent}
                addEventButtonText="project_activity.add_custom_event"
                analysis={analysis}
                onClose={this.closeAddEventForm}
              />
            )}

            {this.state.removeAnalysisForm && (
              <RemoveAnalysisForm
                analysis={analysis}
                deleteAnalysis={this.props.deleteAnalysis}
                onClose={this.closeRemoveAnalysisForm}
              />
            )}
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
