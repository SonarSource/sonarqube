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
import { Dict } from '../../types/types';
import {
  getLocalizedCategoryMetricName,
  getLocalizedMetricDomain,
  getLocalizedMetricName,
  getShortMonthName,
  getShortWeekDayName,
  getWeekDayName,
  hasMessage,
  translate,
  translateWithParameters,
} from '../l10n';
import { getMessages } from '../l10nBundle';

const MSG = 'my_message';

jest.unmock('../l10n');

jest.mock('../l10nBundle', () => ({
  getMessages: jest.fn().mockReturnValue({}),
}));

const resetMessages = (messages: Dict<string>) =>
  (getMessages as jest.Mock).mockReturnValue(messages);

beforeEach(() => {
  resetMessages({});
});

describe('hasMessage', () => {
  it('should return that the message exists', () => {
    resetMessages({
      foo: 'foo',
      'foo.bar': 'foobar',
    });
    expect(hasMessage('foo')).toBe(true);
    expect(hasMessage('foo', 'bar')).toBe(true);
  });

  it('should return that the message is missing', () => {
    expect(hasMessage('foo')).toBe(false);
    expect(hasMessage('foo', 'bar')).toBe(false);
  });
});

describe('translate', () => {
  it('should translate simple message', () => {
    resetMessages({ my_key: MSG });
    expect(translate('my_key')).toBe(MSG);
  });

  it('should translate message with composite key', () => {
    resetMessages({ 'my.composite.message': MSG });
    expect(translate('my', 'composite', 'message')).toBe(MSG);
    expect(translate('my.composite', 'message')).toBe(MSG);
    expect(translate('my', 'composite.message')).toBe(MSG);
    expect(translate('my.composite.message')).toBe(MSG);
  });

  it('should not translate message but return its key', () => {
    expect(translate('random')).toBe('random');
    expect(translate('random', 'key')).toBe('random.key');
    expect(translate('composite.random', 'key')).toBe('composite.random.key');
  });
});

describe('translateWithParameters', () => {
  it('should translate message with one parameter in the beginning', () => {
    resetMessages({ x_apples: '{0} apples' });
    expect(translateWithParameters('x_apples', 5)).toBe('5 apples');
  });

  it('should translate message with one parameter in the middle', () => {
    resetMessages({ x_apples: 'I have {0} apples' });
    expect(translateWithParameters('x_apples', 5)).toBe('I have 5 apples');
  });

  it('should translate message with one parameter in the end', () => {
    resetMessages({ x_apples: 'Apples: {0}' });
    expect(translateWithParameters('x_apples', 5)).toBe('Apples: 5');
  });

  it('should translate message with several parameters', () => {
    resetMessages({
      x_apples: '{0}: I have {2} apples in my {1} baskets - {3}',
    });
    expect(translateWithParameters('x_apples', 1, 2, 3, 4)).toBe(
      '1: I have 3 apples in my 2 baskets - 4'
    );
  });

  it('should not be affected by replacement pattern XSS vulnerability of String.replace', () => {
    resetMessages({ x_apples: 'I have {0} apples' });
    expect(translateWithParameters('x_apples', '$`')).toBe('I have $` apples');
  });

  it('should not translate message but return its key', () => {
    expect(translateWithParameters('random', 5)).toBe('random.5');
    expect(translateWithParameters('random', 1, 2, 3)).toBe('random.1.2.3');
    expect(translateWithParameters('composite.random', 1, 2)).toBe('composite.random.1.2');
  });
});

describe('getLocalizedMetricName', () => {
  const metric = { key: 'new_code', name: 'new_code_metric_name' };

  it('should return the metric name translation', () => {
    resetMessages({ 'metric.new_code.name': 'metric.new_code.name_t' });
    expect(getLocalizedMetricName(metric)).toBe('metric.new_code.name_t');
  });

  it('should return the metric short name', () => {
    resetMessages({
      'metric.new_code.short_name': 'metric.new_code.short_name_t',
    });
    expect(getLocalizedMetricName(metric, true)).toBe('metric.new_code.short_name_t');
  });

  it('should fallback on name if short name is absent', () => {
    resetMessages({ 'metric.new_code.name': 'metric.new_code.name_t' });
    expect(getLocalizedMetricName(metric, true)).toBe('metric.new_code.name_t');
  });

  it('should fallback on metric name if translation is absent', () => {
    expect(getLocalizedMetricName(metric)).toBe('new_code_metric_name');
  });

  it('should fallback on metric key if nothing else is available', () => {
    expect(getLocalizedMetricName({ key: 'new_code' })).toBe('new_code');
  });
});

describe('getLocalizedCategoryMetricName', () => {
  it('should return metric category name translation', () => {
    resetMessages({
      'metric.new_code.extra_short_name': 'metric.new_code.extra_short_name_t',
    });
    expect(getLocalizedCategoryMetricName({ key: 'new_code' })).toBe(
      'metric.new_code.extra_short_name_t'
    );
  });

  it('should fallback on metric name if extra_short_name is absent', () => {
    resetMessages({ 'metric.new_code.name': 'metric.new_code.name_t' });
    expect(getLocalizedCategoryMetricName({ key: 'new_code' })).toBe('metric.new_code.name_t');
  });
});

describe('getLocalizedMetricDomain', () => {
  it('should return metric domain name translation', () => {
    resetMessages({ 'metric_domain.domain': 'metric_domain.domain_t' });
    expect(getLocalizedMetricDomain('domain')).toBe('metric_domain.domain_t');
  });

  it('should fallback on metric domain name', () => {
    expect(getLocalizedMetricDomain('domain')).toBe('domain');
  });
});

describe('getShortMonthName', () => {
  it('should properly translation months', () => {
    resetMessages({ Jan: 'Jan_t' });
    expect(getShortMonthName(0)).toBe('Jan_t');
  });
});

describe('getWeekDayName', () => {
  it('should properly translation weekday', () => {
    resetMessages({ Sunday: 'Sunday_t' });
    expect(getWeekDayName(0)).toBe('Sunday_t');
  });
});

describe('getShortWeekDayName', () => {
  it('should properly translation short weekday', () => {
    resetMessages({ Sun: 'Sun_t' });
    expect(getShortWeekDayName(0)).toBe('Sun_t');
  });
});
