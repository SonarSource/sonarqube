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
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { translate } from '../../helpers/l10n';
import { AnalysisEvent } from '../../types/project-activity';
import Tooltip from '../controls/Tooltip';
import { DefinitionChangeEventInner, isDefinitionChangeEvent } from './DefinitionChangeEventInner';
import { isRichQualityGateEvent, RichQualityGateEventInner } from './RichQualityGateEventInner';

export interface EventInnerProps {
  event: AnalysisEvent;
  readonly?: boolean;
}

export default function EventInner({ event, readonly }: EventInnerProps) {
  if (isRichQualityGateEvent(event)) {
    return <RichQualityGateEventInner event={event} readonly={readonly} />;
  } else if (isDefinitionChangeEvent(event)) {
    return (
      <ComponentContext.Consumer>
        {({ branchLike }) => (
          <DefinitionChangeEventInner branchLike={branchLike} event={event} readonly={readonly} />
        )}
      </ComponentContext.Consumer>
    );
  }

  return (
    <Tooltip overlay={event.description || null}>
      <span className="text-middle">
        <span className="note little-spacer-right">
          {translate('event.category', event.category)}:
        </span>
        <strong className="spacer-right">{event.name}</strong>
      </span>
    </Tooltip>
  );
}
