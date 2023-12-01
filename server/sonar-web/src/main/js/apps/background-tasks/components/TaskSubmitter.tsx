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
import { ContentCell, Note } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DateFormatter from '../../../components/intl/DateFormatter';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { isValidDate, parseDate } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';

interface Props {
  submittedAt: string;
  submitter?: string;
}

export default function TaskSubmitter(props: Readonly<Props>) {
  const { submitter = translate('anonymous'), submittedAt } = props;

  return (
    <ContentCell>
      <div>
        <div className="sw-whitespace-nowrap">
          {isValidDate(parseDate(submittedAt)) ? (
            <FormattedMessage
              id="background_tasks.date_and_time"
              values={{
                date: <DateFormatter date={submittedAt} long />,
                time: <TimeFormatter date={submittedAt} long />,
              }}
            />
          ) : (
            <DateFormatter date={submittedAt} long />
          )}
        </div>
        <Note>
          <FormattedMessage id="background_tasks.submitted_by_x" values={{ submitter }} />
        </Note>
      </div>
    </ContentCell>
  );
}
