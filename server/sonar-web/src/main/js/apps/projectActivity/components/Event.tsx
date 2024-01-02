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
import * as React from 'react';
import EventInner from '../../../components/activity-graph/EventInner';
import { DeleteButton, EditButton } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { AnalysisEvent } from '../../../types/project-activity';
import ChangeEventForm from './forms/ChangeEventForm';
import RemoveEventForm from './forms/RemoveEventForm';

export interface EventProps {
  analysisKey: string;
  canAdmin?: boolean;
  event: AnalysisEvent;
  isFirst?: boolean;
  onChange?: (event: string, name: string) => Promise<void>;
  onDelete?: (analysisKey: string, event: string) => Promise<void>;
}

export function Event(props: EventProps) {
  const { analysisKey, event, canAdmin, isFirst } = props;

  const [changing, setChanging] = React.useState(false);
  const [deleting, setDeleting] = React.useState(false);

  const isOther = event.category === 'OTHER';
  const isVersion = event.category === 'VERSION';
  const canChange = (isOther || isVersion) && props.onChange;
  const canDelete = (isOther || (isVersion && !isFirst)) && props.onDelete;
  const showActions = canAdmin && (canChange || canDelete);

  return (
    <div className="project-activity-event">
      <EventInner event={event} />

      {showActions && (
        <span className="nowrap">
          {canChange && (
            <EditButton
              aria-label={translate('project_activity.events.tooltip.edit')}
              className="button-small"
              data-test="project-activity__edit-event"
              onClick={() => setChanging(true)}
              stopPropagation={true}
            />
          )}
          {canDelete && (
            <DeleteButton
              aria-label={translate('project_activity.events.tooltip.delete')}
              className="button-small"
              data-test="project-activity__delete-event"
              onClick={() => setDeleting(true)}
              stopPropagation={true}
            />
          )}
        </span>
      )}

      {changing && props.onChange && (
        <ChangeEventForm
          changeEvent={props.onChange}
          event={event}
          header={
            isVersion
              ? translate('project_activity.change_version')
              : translate('project_activity.change_custom_event')
          }
          onClose={() => setChanging(false)}
        />
      )}

      {deleting && props.onDelete && (
        <RemoveEventForm
          analysisKey={analysisKey}
          event={event}
          header={
            isVersion
              ? translate('project_activity.remove_version')
              : translate('project_activity.remove_custom_event')
          }
          onClose={() => setDeleting(false)}
          onConfirm={props.onDelete}
          removeEventQuestion={translate(
            `project_activity.${isVersion ? 'remove_version' : 'remove_custom_event'}.question`
          )}
        />
      )}
    </div>
  );
}

export default React.memo(Event);
