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
import { FormattedMessage } from 'react-intl';
import Level from 'sonar-ui-common/components/ui/Level';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isDefinitionChangeEvent } from '../../projectActivity/components/DefinitionChangeEventInner';
import { isRichQualityGateEvent } from '../../projectActivity/components/RichQualityGateEventInner';

interface Props {
  event: T.AnalysisEvent;
}

export default function Event({ event }: Props) {
  if (event.category === 'VERSION') {
    return (
      <span
        className="overview-analysis-event analysis-version"
        title={`${translate('version')} ${event.name}`}>
        {event.name}
      </span>
    );
  }

  const eventCategory = translate('event.category', event.category);
  if (isDefinitionChangeEvent(event)) {
    return (
      <div className="overview-analysis-event">
        <span className="note">{eventCategory}</span>
      </div>
    );
  }

  if (isRichQualityGateEvent(event)) {
    return (
      <div className="overview-analysis-event">
        <span className="note">{eventCategory}:</span>{' '}
        {event.qualityGate.stillFailing ? (
          <FormattedMessage
            defaultMessage={translate('event.quality_gate.still_x')}
            id="event.quality_gate.still_x"
            values={{ status: <Level level={event.qualityGate.status} small={true} /> }}
          />
        ) : (
          <Level level={event.qualityGate.status} small={true} />
        )}
      </div>
    );
  }

  return (
    <div className="overview-analysis-event">
      <span className="note">{eventCategory}:</span>{' '}
      {event.description ? (
        <strong title={event.description}>{event.name}</strong>
      ) : (
        <strong>{event.name}</strong>
      )}
    </div>
  );
}
