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

import {
  ButtonIcon,
  ButtonSize,
  ButtonVariety,
  IconDelete,
  IconEdit,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { useIntl } from 'react-intl';
import EventInner from '../../../components/activity-graph/EventInner';
import { translate } from '../../../helpers/l10n';
import { AnalysisEvent, ProjectAnalysisEventCategory } from '../../../types/project-activity';
import ChangeEventForm from './forms/ChangeEventForm';
import RemoveEventForm from './forms/RemoveEventForm';

export interface EventProps {
  analysisKey: string;
  canAdmin?: boolean;
  event: AnalysisEvent;
  isFirst?: boolean;
  organization: string;
}

function Event(props: Readonly<EventProps>) {
  const { analysisKey, event, canAdmin, isFirst, organization } = props;
  const intl = useIntl();

  const [changing, setChanging] = React.useState(false);
  const [deleting, setDeleting] = React.useState(false);

  const isOther = event.category === ProjectAnalysisEventCategory.Other;
  const isVersion = event.category === ProjectAnalysisEventCategory.Version;
  const canChange = isOther || isVersion;
  const canDelete = isOther || (isVersion && !isFirst);
  const showActions = canAdmin && (canChange || canDelete);

  const editEventLabel = intl.formatMessage(
    { id: 'project_activity.events.tooltip.edit' },
    { event: `${event.category} ${event.name}` },
  );
  const deleteEventLabel = intl.formatMessage(
    { id: 'project_activity.events.tooltip.delete' },
    { event: `${event.category} ${event.name}` },
  );

  return (
    <div className="it__project-activity-event sw-flex sw-justify-between">
      <EventInner event={event} organization={organization} />

      {showActions && (
        <div className="sw-grow-0 sw-shrink-0 sw-ml-2">
          {canChange && (
            <ButtonIcon
              Icon={IconEdit}
              className="-sw-mt-1"
              variety={ButtonVariety.PrimaryGhost}
              size={ButtonSize.Medium}
              tooltipContent={editEventLabel}
              ariaLabel={editEventLabel}
              data-test="project-activity__edit-event"
              onClick={() => setChanging(true)}
            />
          )}
          {canDelete && (
            <ButtonIcon
              className="-sw-mt-1"
              Icon={IconDelete}
              size={ButtonSize.Medium}
              variety={ButtonVariety.DangerGhost}
              tooltipContent={deleteEventLabel}
              ariaLabel={deleteEventLabel}
              data-test="project-activity__delete-event"
              onClick={() => setDeleting(true)}
            />
          )}
        </div>
      )}

      {changing && (
        <ChangeEventForm
          event={event}
          header={
            isVersion
              ? translate('project_activity.change_version')
              : translate('project_activity.change_custom_event')
          }
          onClose={() => setChanging(false)}
        />
      )}

      {deleting && (
        <RemoveEventForm
          analysisKey={analysisKey}
          event={event}
          header={
            isVersion
              ? translate('project_activity.remove_version')
              : translate('project_activity.remove_custom_event')
          }
          onClose={() => setDeleting(false)}
          removeEventQuestion={translate(
            `project_activity.${isVersion ? 'remove_version' : 'remove_custom_event'}.question`,
          )}
        />
      )}
    </div>
  );
}

export default React.memo(Event);
