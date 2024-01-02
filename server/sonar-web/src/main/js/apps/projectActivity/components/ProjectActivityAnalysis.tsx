/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { injectIntl, WrappedComponentProps } from 'react-intl';
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem,
} from '../../../components/controls/ActionsDropdown';
import { ButtonPlain } from '../../../components/controls/buttons';
import ClickEventBoundary from '../../../components/controls/ClickEventBoundary';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { formatterOption } from '../../../components/intl/DateTimeFormatter';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { PopupPlacement } from '../../../components/ui/popups';
import { parseDate } from '../../../helpers/dates';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ParsedAnalysis } from '../../../types/project-activity';
import Events from './Events';
import AddEventForm from './forms/AddEventForm';
import RemoveAnalysisForm from './forms/RemoveAnalysisForm';

export interface ProjectActivityAnalysisProps extends WrappedComponentProps {
  addCustomEvent: (analysis: string, name: string, category?: string) => Promise<void>;
  addVersion: (analysis: string, version: string) => Promise<void>;
  analysis: ParsedAnalysis;
  canAdmin?: boolean;
  canDeleteAnalyses?: boolean;
  canCreateVersion: boolean;
  changeEvent: (event: string, name: string) => Promise<void>;
  deleteAnalysis: (analysis: string) => Promise<void>;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  isBaseline: boolean;
  isFirst: boolean;
  selected: boolean;
  updateSelectedDate: (date: Date) => void;
}

export function ProjectActivityAnalysis(props: ProjectActivityAnalysisProps) {
  let node: HTMLLIElement | null = null;

  const {
    analysis,
    isBaseline,
    isFirst,
    canAdmin,
    canCreateVersion,
    selected,
    intl: { formatDate },
  } = props;

  React.useEffect(() => {
    if (node && selected) {
      node.scrollIntoView({ behavior: 'smooth' });
    }
  });

  const [addEventForm, setAddEventForm] = React.useState(false);
  const [addVersionForm, setAddVersionForm] = React.useState(false);
  const [removeAnalysisForm, setRemoveAnalysisForm] = React.useState(false);

  const parsedDate = parseDate(analysis.date);
  const hasVersion = analysis.events.find((event) => event.category === 'VERSION') != null;

  const canAddVersion = canAdmin && !hasVersion && canCreateVersion;
  const canAddEvent = canAdmin;
  const canDeleteAnalyses =
    props.canDeleteAnalyses && !isFirst && !analysis.manualNewCodePeriodBaseline;

  return (
    <li
      className={classNames('project-activity-analysis bordered-top bordered-bottom', {
        selected,
      })}
      onClick={() => props.updateSelectedDate(analysis.date)}
      ref={(ref) => (node = ref)}
    >
      <div className="display-flex-center display-flex-space-between">
        <div className="project-activity-time">
          <TimeFormatter date={parsedDate} long={false}>
            {(formattedTime) => (
              <ButtonPlain
                aria-current={selected}
                aria-label={translateWithParameters(
                  'project_activity.show_analysis_X_on_graph',
                  analysis.buildString || formatDate(parsedDate, formatterOption)
                )}
                onClick={() => props.updateSelectedDate(analysis.date)}
              >
                <time className="text-middle" dateTime={parsedDate.toISOString()}>
                  {formattedTime}
                </time>
              </ButtonPlain>
            )}
          </TimeFormatter>
        </div>

        {analysis.buildString && (
          <div className="flex-shrink small text-muted text-ellipsis">
            {translateWithParameters(
              'project_activity.analysis_build_string_X',
              analysis.buildString
            )}
          </div>
        )}

        {(canAddVersion || canAddEvent || canDeleteAnalyses) && (
          <ClickEventBoundary>
            <div className="project-activity-analysis-actions big-spacer-left">
              <ActionsDropdown
                label={translateWithParameters(
                  'project_activity.analysis_X_actions',
                  analysis.buildString || formatDate(parsedDate, formatterOption)
                )}
                overlayPlacement={PopupPlacement.BottomRight}
                small={true}
                toggleClassName="js-analysis-actions"
              >
                {canAddVersion && (
                  <ActionsDropdownItem
                    className="js-add-version"
                    onClick={() => setAddVersionForm(true)}
                  >
                    {translate('project_activity.add_version')}
                  </ActionsDropdownItem>
                )}
                {canAddEvent && (
                  <ActionsDropdownItem
                    className="js-add-event"
                    onClick={() => setAddEventForm(true)}
                  >
                    {translate('project_activity.add_custom_event')}
                  </ActionsDropdownItem>
                )}
                {(canAddVersion || canAddEvent) && canDeleteAnalyses && <ActionsDropdownDivider />}
                {canDeleteAnalyses && (
                  <ActionsDropdownItem
                    className="js-delete-analysis"
                    destructive={true}
                    onClick={() => setRemoveAnalysisForm(true)}
                  >
                    {translate('project_activity.delete_analysis')}
                  </ActionsDropdownItem>
                )}
              </ActionsDropdown>

              {addVersionForm && (
                <AddEventForm
                  addEvent={props.addVersion}
                  addEventButtonText="project_activity.add_version"
                  analysis={analysis}
                  onClose={() => setAddVersionForm(false)}
                />
              )}

              {addEventForm && (
                <AddEventForm
                  addEvent={props.addCustomEvent}
                  addEventButtonText="project_activity.add_custom_event"
                  analysis={analysis}
                  onClose={() => setAddEventForm(false)}
                />
              )}

              {removeAnalysisForm && (
                <RemoveAnalysisForm
                  analysis={analysis}
                  deleteAnalysis={props.deleteAnalysis}
                  onClose={() => setRemoveAnalysisForm(false)}
                />
              )}
            </div>
          </ClickEventBoundary>
        )}
      </div>

      {analysis.events.length > 0 && (
        <Events
          analysisKey={analysis.key}
          canAdmin={canAdmin}
          events={analysis.events}
          isFirst={isFirst}
          onChange={props.changeEvent}
          onDelete={props.deleteEvent}
        />
      )}

      {isBaseline && (
        <div className="baseline-marker">
          <div className="wedge" />
          <hr />
          <div className="label display-flex-center">
            {translate('project_activity.new_code_period_start')}
            <HelpTooltip
              className="little-spacer-left"
              overlay={translate('project_activity.new_code_period_start.help')}
              placement="top"
            />
          </div>
        </div>
      )}
    </li>
  );
}

export default injectIntl(ProjectActivityAnalysis);
