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
import * as React from 'react';
import * as classNames from 'classnames';
import { isRichQualityGateEvent, RichQualityGateEventInner } from './RichQualityGateEventInner';
import { isDefinitionChangeEvent, DefinitionChangeEventInner } from './DefinitionChangeEventInner';
import { ComponentContext } from '../../../app/components/ComponentContext';
import ProjectEventIcon from '../../../components/icons-components/ProjectEventIcon';
import { translate } from '../../../helpers/l10n';

interface Props {
  event: T.AnalysisEvent;
}

export default function EventInner({ event }: Props) {
  if (isRichQualityGateEvent(event)) {
    return <RichQualityGateEventInner event={event} />;
  } else if (isDefinitionChangeEvent(event)) {
    return (
      <ComponentContext.Consumer>
        {({ branchLike }) => <DefinitionChangeEventInner branchLike={branchLike} event={event} />}
      </ComponentContext.Consumer>
    );
  } else {
    return (
      <div className="project-activity-event-inner">
        <div className="project-activity-event-inner-main">
          <ProjectEventIcon
            className={classNames(
              'project-activity-event-icon',
              'little-spacer-right',
              event.category
            )}
          />

          <span className="project-activity-event-inner-text">
            <span className="note little-spacer-right">
              {translate('event.category', event.category)}:
            </span>
            <strong title={event.description}>{event.name}</strong>
          </span>
        </div>
      </div>
    );
  }
}
