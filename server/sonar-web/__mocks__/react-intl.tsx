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
import { isObject, some } from 'lodash';
import * as React from 'react';

module.exports = {
  ...jest.requireActual('react-intl'),
  useIntl: () => ({
    formatMessage: ({ id }, values = {}) => {
      if (some(values, isObject)) {
        return (
          <>
            {id}
            {Object.entries(values).map(([key, value]) => (
              <React.Fragment key={key}>
                {typeof value === 'function' ? value() : value}
              </React.Fragment>
            ))}
          </>
        );
      }
      return [id, ...Object.values(values)].join('.');
    },
    formatDate: jest.fn().mockReturnValue(''),
  }),
  FormattedMessage: ({
    id,
    values,
  }: {
    id: string;
    values?: { [x: string]: React.ReactNode | (() => React.ReactNode) };
  }) => {
    return (
      <>
        {id}
        {values !== undefined &&
          Object.entries(values).map(([key, value]) => (
            <React.Fragment key={key}>
              {typeof value === 'function' ? value() : value}
            </React.Fragment>
          ))}
      </>
    );
  },
};
