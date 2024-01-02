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
import { DestructiveIcon, InteractiveIcon, PencilIcon, TrashIcon } from 'design-system';
import * as React from 'react';
import EventInner from '../../../components/activity-graph/EventInner';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { AnalysisEvent, ProjectAnalysisEventCategory } from '../../../types/project-activity';
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

function Event(props: EventProps) {
  const { analysisKey, event, canAdmin, isFirst } = props;

  const [changing, setChanging] = React.useState(false);
  const [deleting, setDeleting] = React.useState(false);

  const isOther = event.category === ProjectAnalysisEventCategory.Other;
  const isVersion = event.category === ProjectAnalysisEventCategory.Version;
  const canChange = (isOther || isVersion) && props.onChange;
  const canDelete = (isOther || (isVersion && !isFirst)) && props.onDelete;
  const showActions = canAdmin && (canChange || canDelete);

  return (
    <div className="it__project-activity-event sw-flex sw-justify-between">
      <EventInner event={event} />

      {showActions && (
        <div className="sw-grow-0 sw-shrink-0 sw-ml-2">
          {canChange && (
            <Tooltip overlay={translate('project_activity.events.tooltip.edit')}>
              <InteractiveIcon
                Icon={PencilIcon}
                aria-label={translate('project_activity.events.tooltip.edit')}
                data-test="project-activity__edit-event"
                onClick={() => setChanging(true)}
                stopPropagation
                size="small"
              />
            </Tooltip>
          )}
          {canDelete && (
            <Tooltip overlay={translate('project_activity.events.tooltip.delete')}>
              <DestructiveIcon
                Icon={TrashIcon}
                aria-label={translate('project_activity.events.tooltip.delete')}
                data-test="project-activity__delete-event"
                onClick={() => setDeleting(true)}
                stopPropagation
                size="small"
              />
            </Tooltip>
          )}
        </div>
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
            `project_activity.${isVersion ? 'remove_version' : 'remove_custom_event'}.question`,
          )}
        />
      )}
    </div>
  );
}

export default React.memo(Event);
