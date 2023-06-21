/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Note } from 'design-system';
import * as React from 'react';
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { translate } from '../../helpers/l10n';
import { AnalysisEvent } from '../../types/project-activity';
import Tooltip from '../controls/Tooltip';
import { DefinitionChangeEventInner, isDefinitionChangeEvent } from './DefinitionChangeEventInner';
import { RichQualityGateEventInner, isRichQualityGateEvent } from './RichQualityGateEventInner';

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
    <Tooltip overlay={event.description}>
      <div className="sw-min-w-0 sw-flex-1">
        <div className="sw-flex sw-items-start">
          <span>
            <Note className="sw-mr-1 sw-body-sm-highlight">
              {translate('event.category', event.category)}
              {event.category === 'VERSION' && ':'}
            </Note>
            <Note className="sw-body-sm" title={event.description}>
              {event.name}
            </Note>
          </span>
        </div>
      </div>
    </Tooltip>
  );
}
