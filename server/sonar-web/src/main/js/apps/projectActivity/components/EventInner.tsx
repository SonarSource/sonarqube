/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { ComponentContext } from '../../../app/components/ComponentContext';
import { DefinitionChangeEventInner, isDefinitionChangeEvent } from './DefinitionChangeEventInner';
import { isRichQualityGateEvent, RichQualityGateEventInner } from './RichQualityGateEventInner';

export interface EventInnerProps {
  event: T.AnalysisEvent;
}

export default function EventInner({ event }: EventInnerProps) {
  if (isRichQualityGateEvent(event)) {
    return <RichQualityGateEventInner event={event} />;
  } else if (isDefinitionChangeEvent(event)) {
    return (
      <ComponentContext.Consumer>
        {({ branchLike }) => <DefinitionChangeEventInner branchLike={branchLike} event={event} />}
      </ComponentContext.Consumer>
    );
  } else {
    const content = (
      <span className="text-middle">
        <span className="note little-spacer-right">
          {translate('event.category', event.category)}:
        </span>
        <strong className="spacer-right">{event.name}</strong>
      </span>
    );
    return event.description ? <Tooltip overlay={event.description}>{content}</Tooltip> : content;
  }
}
