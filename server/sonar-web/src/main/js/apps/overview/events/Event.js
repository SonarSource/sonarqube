/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import moment from 'moment';

import { EventType } from '../propTypes';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { translate } from '../../../helpers/l10n';

const Event = ({ event }) => {
  return (
      <TooltipsContainer>
        <li className="spacer-top">
          <p>
            <strong className="js-event-type">
              {translate('event.category', event.type)}
            </strong>
            {': '}
            <span className="js-event-name">{event.name}</span>
            {event.text && (
                <i
                    className="spacer-left icon-help"
                    data-toggle="tooltip"
                    title={event.text}/>
            )}
          </p>
          <p className="note little-spacer-top js-event-date">
            {moment(event.date).format('LL')}
          </p>
        </li>
      </TooltipsContainer>
  );
};

Event.propTypes = {
  event: EventType.isRequired
};

export default Event;
