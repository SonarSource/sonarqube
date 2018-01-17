/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { resetBundle } from '../l10n';
import { formatMeasure } from '../measures';

const HOURS_IN_DAY = 8;
const ONE_MINUTE = 1;
const ONE_HOUR = ONE_MINUTE * 60;
const ONE_DAY = HOURS_IN_DAY * ONE_HOUR;

beforeEach(() => {
  resetBundle({
    'work_duration.x_days': '{0}d',
    'work_duration.x_hours': '{0}h',
    'work_duration.x_minutes': '{0}min',
    'work_duration.about': '~ {0}',
    'metric.level.ERROR': 'Error',
    'metric.level.WARN': 'Warning',
    'metric.level.OK': 'Ok',
    'short_number_suffix.g': 'G',
    'short_number_suffix.k': 'k',
    'short_number_suffix.m': 'M'
  });
});

describe('#formatMeasure()', () => {
  it('should format INT', () => {
    expect(formatMeasure(0, 'INT')).toBe('0');
    expect(formatMeasure(1, 'INT')).toBe('1');
    expect(formatMeasure(-5, 'INT')).toBe('-5');
    expect(formatMeasure(999, 'INT')).toBe('999');
    expect(formatMeasure(1000, 'INT')).toBe('1,000');
    expect(formatMeasure(1529, 'INT')).toBe('1,529');
    expect(formatMeasure(10000, 'INT')).toBe('10,000');
    expect(formatMeasure(1234567890, 'INT')).toBe('1,234,567,890');
  });

  it('should format SHORT_INT', () => {
    expect(formatMeasure(0, 'SHORT_INT')).toBe('0');
    expect(formatMeasure(1, 'SHORT_INT')).toBe('1');
    expect(formatMeasure(999, 'SHORT_INT')).toBe('999');
    expect(formatMeasure(1000, 'SHORT_INT')).toBe('1k');
    expect(formatMeasure(1529, 'SHORT_INT')).toBe('1.5k');
    expect(formatMeasure(10000, 'SHORT_INT')).toBe('10k');
    expect(formatMeasure(10678, 'SHORT_INT')).toBe('11k');
    expect(formatMeasure(1234567890, 'SHORT_INT')).toBe('1G');
  });

  it('should format FLOAT', () => {
    expect(formatMeasure(0.0, 'FLOAT')).toBe('0.0');
    expect(formatMeasure(1.0, 'FLOAT')).toBe('1.0');
    expect(formatMeasure(1.3, 'FLOAT')).toBe('1.3');
    expect(formatMeasure(1.34, 'FLOAT')).toBe('1.34');
    expect(formatMeasure(50.89, 'FLOAT')).toBe('50.89');
    expect(formatMeasure(100.0, 'FLOAT')).toBe('100.0');
    expect(formatMeasure(123.456, 'FLOAT')).toBe('123.456');
    expect(formatMeasure(123456.7, 'FLOAT')).toBe('123,456.7');
    expect(formatMeasure(1234567890.0, 'FLOAT')).toBe('1,234,567,890.0');
  });

  it('should respect FLOAT precision', () => {
    expect(formatMeasure(0.1, 'FLOAT')).toBe('0.1');
    expect(formatMeasure(0.12, 'FLOAT')).toBe('0.12');
    expect(formatMeasure(0.12345, 'FLOAT')).toBe('0.12345');
    expect(formatMeasure(0.123456, 'FLOAT')).toBe('0.12346');
  });

  it('should format PERCENT', () => {
    expect(formatMeasure(0.0, 'PERCENT')).toBe('0.0%');
    expect(formatMeasure(1.0, 'PERCENT')).toBe('1.0%');
    expect(formatMeasure(1.3, 'PERCENT')).toBe('1.3%');
    expect(formatMeasure(1.34, 'PERCENT')).toBe('1.3%');
    expect(formatMeasure(50.89, 'PERCENT')).toBe('50.9%');
    expect(formatMeasure(100.0, 'PERCENT')).toBe('100%');
    expect(formatMeasure(50.89, 'PERCENT', { decimals: 0 })).toBe('50.9%');
    expect(formatMeasure(50.89, 'PERCENT', { decimals: 1 })).toBe('50.9%');
    expect(formatMeasure(50.89, 'PERCENT', { decimals: 2 })).toBe('50.89%');
    expect(formatMeasure(50.89, 'PERCENT', { decimals: 3 })).toBe('50.890%');
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
    expect(formatMeasure(0, 'SHORT_WORK_DUR')).toBe('0');
    expect(formatMeasure(5 * ONE_DAY, 'SHORT_WORK_DUR')).toBe('5d');
    expect(formatMeasure(2 * ONE_HOUR, 'SHORT_WORK_DUR')).toBe('2h');
    expect(formatMeasure(ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('1min');
    expect(formatMeasure(40 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('40min');
    expect(formatMeasure(58 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('1h');
    expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).toBe('5d');
    expect(formatMeasure(2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('2h');
    expect(formatMeasure(ONE_HOUR + 55 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('2h');
    expect(formatMeasure(3 * ONE_DAY + 6 * ONE_HOUR, 'SHORT_WORK_DUR')).toBe('4d');
    expect(formatMeasure(7 * ONE_HOUR + 59 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('1d');
    expect(formatMeasure(5 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('5d');
    expect(formatMeasure(15 * ONE_DAY + 2 * ONE_HOUR + ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('15d');
    expect(formatMeasure(7 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('7min');
    expect(formatMeasure(-5 * ONE_DAY, 'SHORT_WORK_DUR')).toBe('-5d');
    expect(formatMeasure(-2 * ONE_HOUR, 'SHORT_WORK_DUR')).toBe('-2h');
    expect(formatMeasure(-1 * ONE_MINUTE, 'SHORT_WORK_DUR')).toBe('-1min');

    expect(formatMeasure(1529 * ONE_DAY, 'SHORT_WORK_DUR')).toBe('1.5kd');
    expect(formatMeasure(1234567 * ONE_DAY, 'SHORT_WORK_DUR')).toBe('1Md');
    expect(formatMeasure(1234567 * ONE_DAY + 2 * ONE_HOUR, 'SHORT_WORK_DUR')).toBe('1Md');
  });

  it('should format RATING', () => {
    expect(formatMeasure(1, 'RATING')).toBe('A');
    expect(formatMeasure(2, 'RATING')).toBe('B');
    expect(formatMeasure(3, 'RATING')).toBe('C');
    expect(formatMeasure(4, 'RATING')).toBe('D');
    expect(formatMeasure(5, 'RATING')).toBe('E');
  });

  it('should format LEVEL', () => {
    expect(formatMeasure('ERROR', 'LEVEL')).toBe('Error');
    expect(formatMeasure('WARN', 'LEVEL')).toBe('Warning');
    expect(formatMeasure('OK', 'LEVEL')).toBe('Ok');
    expect(formatMeasure('UNKNOWN', 'LEVEL')).toBe('UNKNOWN');
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
    expect(formatMeasure('', 'PERCENT')).toBe('');
  });

  it('should not fail with undefined', () => {
    expect(formatMeasure(undefined, 'INT')).toBe('');
  });
});
