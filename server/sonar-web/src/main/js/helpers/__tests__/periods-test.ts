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
import { getPeriodLabel } from '../periods';
import { mockPeriod } from '../testMocks';

const formatter = jest.fn((v) => v);

beforeEach(() => {
  formatter.mockClear();
});

describe('getPeriodLabel', () => {
  it('should handle missing value', () => {
    expect(getPeriodLabel(undefined, formatter)).toBeUndefined();
  });

  it('should handle date', () => {
    expect(getPeriodLabel(mockPeriod({ mode: 'date' }), formatter)).toBe('overview.period.date.');
    expect(
      getPeriodLabel(mockPeriod({ mode: 'date', parameter: '2019-02-21T01:11:21+0100' }), formatter)
    ).toBe('overview.period.date.2019-02-21T01:11:21+0100');
    expect(formatter).toHaveBeenCalledTimes(1);
  });

  it('should handle days', () => {
    expect(getPeriodLabel(mockPeriod({ mode: 'days', modeParam: '12' }), formatter)).toBe(
      'overview.period.days.12'
    );
    expect(formatter).not.toHaveBeenCalled();
  });

  it('should handle previous analysis', () => {
    expect(
      getPeriodLabel(mockPeriod({ mode: 'previous_analysis', parameter: 'param' }), formatter)
    ).toBe('overview.period.previous_analysis.param');
    expect(formatter).not.toHaveBeenCalled();
  });

  it('should handle previous version', () => {
    expect(getPeriodLabel(mockPeriod({ mode: 'previous_version' }), formatter)).toBe(
      'overview.period.previous_version_only_date'
    );
    expect(
      getPeriodLabel(mockPeriod({ mode: 'previous_version', parameter: '7.9' }), formatter)
    ).toBe('overview.period.previous_version.7.9');
    expect(formatter).not.toHaveBeenCalled();
  });

  it('should handle version', () => {
    expect(getPeriodLabel(mockPeriod({ mode: 'version', modeParam: '7.2' }), formatter)).toBe(
      'overview.period.version.7.2'
    );
    expect(
      getPeriodLabel(mockPeriod({ mode: 'previous_version', parameter: '7.9' }), formatter)
    ).toBe('overview.period.previous_version.7.9');
    expect(formatter).not.toHaveBeenCalled();
  });

  it('should handle manual baseline', () => {
    expect(
      getPeriodLabel(mockPeriod({ mode: 'manual_baseline', modeParam: 'A658678DE' }), formatter)
    ).toBe('overview.period.manual_baseline.A658678DE');
    expect(getPeriodLabel(mockPeriod({ mode: 'manual_baseline' }), formatter)).toBe(
      'overview.period.manual_baseline.2019-04-23T02:12:32+0100'
    );
    expect(formatter).toHaveBeenCalledTimes(1);
  });

  it('should handle SPECIFIC_ANALYSIS', () => {
    expect(
      getPeriodLabel(mockPeriod({ mode: 'SPECIFIC_ANALYSIS', parameter: '7.1' }), formatter)
    ).toBe('overview.period.specific_analysis.2019-04-23T02:12:32+0100');
    expect(getPeriodLabel(mockPeriod({ mode: 'SPECIFIC_ANALYSIS' }), formatter)).toBe(
      'overview.period.specific_analysis.2019-04-23T02:12:32+0100'
    );
    expect(formatter).toHaveBeenCalledTimes(2);
  });

  it('should handle PREVIOUS_VERSION', () => {
    expect(
      getPeriodLabel(mockPeriod({ mode: 'PREVIOUS_VERSION', modeParam: 'A658678DE' }), formatter)
    ).toBe('overview.period.previous_version.A658678DE');
    expect(getPeriodLabel(mockPeriod({ mode: 'PREVIOUS_VERSION' }), formatter)).toBe(
      'overview.period.previous_version.2019-04-23T02:12:32+0100'
    );
    expect(formatter).toHaveBeenCalledTimes(1);
  });
});
