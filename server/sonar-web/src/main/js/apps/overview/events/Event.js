/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import type { Event as EventType } from '../../projectActivity/types';
import { translate } from '../../../helpers/l10n';

export default function Event(props: { event: EventType }) {
  const { event } = props;

  if (event.category === 'VERSION') {
    return <span className="overview-analysis-event badge">{props.event.name}</span>;
  }

  return (
    <div className="overview-analysis-event">
      <TooltipsContainer>
        <span>
          <span className="note">{translate('event.category', event.category)}:</span>
          {' '}
          <strong title={event.description} data-toggle="tooltip">{event.name}</strong>
        </span>
      </TooltipsContainer>
    </div>
  );
}
