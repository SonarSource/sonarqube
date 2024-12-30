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

import { IntlShape } from 'react-intl';
import { MetricType } from '~sonar-aligned/types/metrics';
import { getIntl, getMessages } from '../../../helpers/l10nBundle';
import { Dict } from '../../../types/types';
import { formatMeasure } from '../measures';

const HOURS_IN_DAY = 8;
const ONE_MINUTE = 1;
const ONE_HOUR = ONE_MINUTE * 60;
const ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

jest.unmock('../../../helpers/l10n');

jest.mock('../../../helpers/l10nBundle', () => ({
  getCurrentLocale: jest.fn().mockReturnValue('us'),
  getMessages: jest.fn().mockReturnValue({}),
  getIntl: jest.fn().mockReturnValue({ formatMessage: jest.fn(({ id }) => `${id}`) }),
}));

const resetMessages = (messages: Dict<string>) => {
  jest.mocked(getMessages).mockReturnValue(messages);

  jest.mocked(getIntl).mockReturnValue({
    formatMessage: jest.fn(({ id }) => {
      return id ? (messages[id] ?? id) : `${id}`;
    }),
  } as unknown as IntlShape);
};

beforeAll(() => {
  resetMessages({
    'work_duration.x_days': '{0}d',
    'work_duration.x_hours': '{0}h',
    'work_duration.x_minutes': '{0}min',
    'work_duration.about': '~ {0}',
    'metric.level.ERROR': 'Error',
    'metric.level.WARN': 'Warning',
    'metric.level.OK': 'Ok',
    'short_number_suffix.g': 'G',
    'short_number_suffix.k': 'k',
    'short_number_suffix.m': 'M',
  });
});

describe('#formatMeasure()', () => {
  it('should format INT', () => {
    expect(formatMeasure(0, MetricType.Integer)).toBe('0');
    expect(formatMeasure(1, MetricType.Integer)).toBe('1');
    expect(formatMeasure(-5, MetricType.Integer)).toBe('-5');
    expect(formatMeasure(999, MetricType.Integer)).toBe('999');
    expect(formatMeasure(1000, MetricType.Integer)).toBe('1,000');
    expect(formatMeasure(1529, MetricType.Integer)).toBe('1,529');
    expect(formatMeasure(10000, MetricType.Integer)).toBe('10,000');
    expect(formatMeasure(1234567890, MetricType.Integer)).toBe('1,234,567,890');
  });

  it('should format SHORT_INT', () => {
    expect(formatMeasure(0, MetricType.ShortInteger)).toBe('0');
    expect(formatMeasure(1, MetricType.ShortInteger)).toBe('1');
    expect(formatMeasure(999, MetricType.ShortInteger)).toBe('999');
    expect(formatMeasure(1000, MetricType.ShortInteger)).toBe('1k');
    expect(formatMeasure(1529, MetricType.ShortInteger)).toBe('1.5k');
    expect(formatMeasure(10000, MetricType.ShortInteger)).toBe('10k');
    expect(formatMeasure(10678, MetricType.ShortInteger)).toBe('11k');
    expect(formatMeasure(9467890, MetricType.ShortInteger)).toBe('9.5M');
    expect(formatMeasure(994567890, MetricType.ShortInteger)).toBe('995M');
    expect(formatMeasure(999000001, MetricType.ShortInteger)).toBe('999M');
    expect(formatMeasure(999567890, MetricType.ShortInteger)).toBe('1G');
    expect(formatMeasure(1234567890, MetricType.ShortInteger)).toBe('1.2G');
    expect(formatMeasure(11234567890, MetricType.ShortInteger)).toBe('11G');
  });

  it('should format FLOAT', () => {
    expect(formatMeasure(0, 'FLOAT')).toBe('0.0');
    expect(formatMeasure(1, 'FLOAT')).toBe('1.0');
    expect(formatMeasure(1.3, 'FLOAT')).toBe('1.3');
    expect(formatMeasure(1.34, 'FLOAT')).toBe('1.34');
    expect(formatMeasure(50.89, 'FLOAT')).toBe('50.89');
    expect(formatMeasure(100, 'FLOAT')).toBe('100.0');
    expect(formatMeasure(123.456, 'FLOAT')).toBe('123.456');
    expect(formatMeasure(123456.7, 'FLOAT')).toBe('123,456.7');
    expect(formatMeasure(1234567890, 'FLOAT')).toBe('1,234,567,890.0');
  });

  it('should respect FLOAT precision', () => {
    expect(formatMeasure(0.1, 'FLOAT')).toBe('0.1');
    expect(formatMeasure(0.12, 'FLOAT')).toBe('0.12');
    expect(formatMeasure(0.12345, 'FLOAT')).toBe('0.12345');
    expect(formatMeasure(0.123456, 'FLOAT')).toBe('0.12346');
  });

  it('should format PERCENT', () => {
    expect(formatMeasure(0, MetricType.Percent)).toBe('0.0%');
    expect(formatMeasure(1, MetricType.Percent)).toBe('1.0%');
    expect(formatMeasure(1.3, MetricType.Percent)).toBe('1.3%');
    expect(formatMeasure(1.34, MetricType.Percent)).toBe('1.3%');
    expect(formatMeasure(50.89, MetricType.Percent)).toBe('50.9%');
    expect(formatMeasure(100, MetricType.Percent)).toBe('100%');
    expect(formatMeasure(50.89, MetricType.Percent, { decimals: 0 })).toBe('50.9%');
    expect(formatMeasure(50.89, MetricType.Percent, { decimals: 1 })).toBe('50.9%');
    expect(formatMeasure(50.89, MetricType.Percent, { decimals: 2 })).toBe('50.89%');
    expect(formatMeasure(50.89, MetricType.Percent, { decimals: 3 })).toBe('50.890%');
    expect(
      formatMeasure(50, MetricType.Percent, { decimals: 0, omitExtraDecimalZeros: true }),
    ).toBe('50.0%');
    expect(
      formatMeasure(50, MetricType.Percent, { decimals: 1, omitExtraDecimalZeros: true }),
    ).toBe('50.0%');
    expect(
      formatMeasure(50, MetricType.Percent, { decimals: 3, omitExtraDecimalZeros: true }),
    ).toBe('50.0%');
    expect(
      formatMeasure(50.89, MetricType.Percent, { decimals: 3, omitExtraDecimalZeros: true }),
    ).toBe('50.89%');
  });

  it('should format WORK_DUR', () => {
    expect(formatMeasure(0, 'WORK_DUR')).toBe('0');
    expect(formatMeasure(5 * ONE_DAY, 'WORK_DUR')).toBe('5d');
    expect(formatMeasure(2 * ONE_HOUR, 'WORK_DUR')).toBe('2h');
    expect(formatMeasure(40 * ONE_MINUTE, 'WORK_DUR')).toBe('40min');
    expect(formatMeasure(ONE_MINUTE, 'WORK_DUR')).toBe('1min');
    expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'WORK_DUR')).toBe('5d 2h');
    expect(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).toBe('2h 1min');
    expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).toBe('5d 2h');
    expect(formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'WORK_DUR')).toBe('15d');
    expect(formatMeasure(-5 * ONE_DAY, 'WORK_DUR')).toBe('-5d');
    expect(formatMeasure(-2 * ONE_HOUR, 'WORK_DUR')).toBe('-2h');
    expect(formatMeasure(-1 * ONE_MINUTE, 'WORK_DUR')).toBe('-1min');
  });

  it('should format SHORT_WORK_DUR', () => {
    expect(formatMeasure(0, MetricType.ShortWorkDuration)).toBe('0');
    expect(formatMeasure(5 * ONE_DAY, MetricType.ShortWorkDuration)).toBe('5d');
    expect(formatMeasure(2 * ONE_HOUR, MetricType.ShortWorkDuration)).toBe('2h');
    expect(formatMeasure(ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('1min');
    expect(formatMeasure(40 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('40min');
    expect(formatMeasure(58 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('1h');
    expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, MetricType.ShortWorkDuration)).toBe('5d');
    expect(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('2h');
    expect(formatMeasure(ONE_HOUR + 55 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('2h');
    expect(formatMeasure(3 * ONE_DAY + 6 * ONE_HOUR, MetricType.ShortWorkDuration)).toBe('4d');
    expect(formatMeasure(7 * ONE_HOUR + 59 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('1d');
    expect(
      formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, MetricType.ShortWorkDuration),
    ).toBe('5d');
    expect(
      formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, MetricType.ShortWorkDuration),
    ).toBe('15d');
    expect(formatMeasure(7 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('7min');
    expect(formatMeasure(-5 * ONE_DAY, MetricType.ShortWorkDuration)).toBe('-5d');
    expect(formatMeasure(-2 * ONE_HOUR, MetricType.ShortWorkDuration)).toBe('-2h');
    expect(formatMeasure(-1 * ONE_MINUTE, MetricType.ShortWorkDuration)).toBe('-1min');

    expect(formatMeasure(1529 * ONE_DAY, MetricType.ShortWorkDuration)).toBe('1.5kd');
    expect(formatMeasure(1234567 * ONE_DAY, MetricType.ShortWorkDuration)).toBe('1.2Md');
    expect(formatMeasure(12345670 * ONE_DAY + 4 * ONE_HOUR, MetricType.ShortWorkDuration)).toBe(
      '12Md',
    );
  });

  it('should format RATING', () => {
    expect(formatMeasure(1, MetricType.Rating)).toBe('A');
    expect(formatMeasure(2, MetricType.Rating)).toBe('B');
    expect(formatMeasure(3, MetricType.Rating)).toBe('C');
    expect(formatMeasure(4, MetricType.Rating)).toBe('D');
    expect(formatMeasure(5, MetricType.Rating)).toBe('E');
  });

  it('should format LEVEL', () => {
    expect(formatMeasure('ERROR', MetricType.Level)).toBe('Error');
    expect(formatMeasure('WARN', MetricType.Level)).toBe('Warning');
    expect(formatMeasure('OK', MetricType.Level)).toBe('Ok');
    expect(formatMeasure('UNKNOWN', MetricType.Level)).toBe('UNKNOWN');
  });

  it('should format MILLISEC', () => {
    expect(formatMeasure(0, 'MILLISEC')).toBe('0ms');
    expect(formatMeasure(1, 'MILLISEC')).toBe('1ms');
    expect(formatMeasure(173, 'MILLISEC')).toBe('173ms');
    expect(formatMeasure(3649, 'MILLISEC')).toBe('4s');
    expect(formatMeasure(893481, 'MILLISEC')).toBe('15min');
    expect(formatMeasure(17862325, 'MILLISEC')).toBe('298min');
  });

  it('should not format unknown type', () => {
    expect(formatMeasure('random value', 'RANDOM_TYPE')).toBe('random value');
  });

  it('should return null if value is empty string', () => {
    expect(formatMeasure('', MetricType.Percent)).toBe('');
  });

  it('should not fail with undefined', () => {
    expect(formatMeasure(undefined, MetricType.Integer)).toBe('');
  });
});
