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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { ParsedAnalysis } from '../utils';
import Events from './Events';
import AddEventForm from './forms/AddEventForm';
import RemoveAnalysisForm from './forms/RemoveAnalysisForm';

interface Props {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<void>;
  addVersion: (analysis: string, version: string) => Promise<void>;
  analysis: ParsedAnalysis;
  canAdmin?: boolean;
  canDeleteAnalyses?: boolean;
  canCreateVersion: boolean;
  changeEvent: (event: string, name: string) => Promise<void>;
  deleteAnalysis: (analysis: string) => Promise<void>;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  isFirst: boolean;
  selected: boolean;
  updateSelectedDate: (date: Date) => void;
}

interface State {
  addEventForm: boolean;
  addVersionForm: boolean;
  removeAnalysisForm: boolean;
  suppressVersionTooltip?: boolean;
}

export default class ProjectActivityAnalysis extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    addEventForm: false,
    addVersionForm: false,
    removeAnalysisForm: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = () => {
    this.props.updateSelectedDate(this.props.analysis.date);
  };

  stopPropagation = (event: React.SyntheticEvent) => {
    event.stopPropagation();
  };

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

  handleTimeTooltipHide = () => {
    this.setState({ suppressVersionTooltip: false });
  };

  handleTimeTooltipShow = () => {
    this.setState({ suppressVersionTooltip: true });
  };

  closeAddVersionForm = () => {
    if (this.mounted) {
      this.setState({ addVersionForm: false });
    }
  };

  render() {
    const { analysis, isFirst, canAdmin, canCreateVersion } = this.props;
    const { date, events } = analysis;
    const parsedDate = parseDate(date);
    const hasVersion = events.find(event => event.category === 'VERSION') != null;

    const canAddVersion = canAdmin && !hasVersion && canCreateVersion;
    const canAddEvent = canAdmin;
    const canDeleteAnalyses =
      this.props.canDeleteAnalyses && !isFirst && !analysis.manualNewCodePeriodBaseline;

    let tooltipContent = <TimeFormatter date={parsedDate} long={true} />;
    if (analysis.buildString) {
      tooltipContent = (
        <>
          {tooltipContent}
          <br />
          {translateWithParameters(
            'project_activity.analysis_build_string_X',
            analysis.buildString
          )}
        </>
      );
    }

    return (
      <Tooltip mouseEnterDelay={0.5} overlay={tooltipContent} placement="left">
        <li
          className={classNames('project-activity-analysis', { selected: this.props.selected })}
          data-date={parsedDate.valueOf()}
          onClick={this.handleClick}
          tabIndex={0}>
          <div className="project-activity-time spacer-right">
            <TimeFormatter date={parsedDate} long={false}>
              {formattedTime => (
                <time className="text-middle" dateTime={parsedDate.toISOString()}>
                  {formattedTime}
                </time>
              )}
            </TimeFormatter>
          </div>

          {(canAddVersion || canAddEvent || canDeleteAnalyses) && (
            <div className="project-activity-analysis-actions big-spacer-right">
              <ActionsDropdown small={true} toggleClassName="js-analysis-actions">
                {canAddVersion && (
                  <ActionsDropdownItem
                    className="js-add-event"
                    onClick={this.handleAddVersionClick}>
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
              changeEvent={this.props.changeEvent}
              deleteEvent={this.props.deleteEvent}
              events={events}
              isFirst={this.props.isFirst}
            />
          )}
        </li>
      </Tooltip>
    );
  }
}
