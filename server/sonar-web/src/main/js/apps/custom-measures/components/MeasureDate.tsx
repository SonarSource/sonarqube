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
import DateFormatter from '../../../components/intl/DateFormatter';
import { translate } from '../../../helpers/l10n';

interface Props {
  measure: T.CustomMeasure;
}

export default function MeasureDate({ measure }: Props) {
  if (measure.updatedAt) {
    return (
      <>
        {translate('updated_on')}{' '}
        <span className="js-custom-measure-created-at">
          <DateFormatter date={measure.updatedAt} />
        </span>
      </>
    );
  } else if (measure.createdAt) {
    return (
      <>
        {translate('created_on')}{' '}
        <span className="js-custom-measure-created-at">
          <DateFormatter date={measure.createdAt} />
        </span>
      </>
    );
  } else {
    return <>{translate('created')}</>;
  }
}
