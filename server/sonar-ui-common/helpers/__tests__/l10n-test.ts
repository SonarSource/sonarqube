/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
/* eslint-disable camelcase */
import Initializer, { getMessages } from '../init';
import { hasMessage, translate, translateWithParameters } from '../l10n';

const originalMessages = getMessages();
const MSG = 'my_message';

afterEach(() => {
  Initializer.setMessages(originalMessages);
});

describe('translate', () => {
  it('should translate simple message', () => {
    Initializer.setMessages({ my_key: MSG });
    expect(translate('my_key')).toBe(MSG);
  });

  it('should translate message with composite key', () => {
    Initializer.setMessages({ 'my.composite.message': MSG });
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
    Initializer.setMessages({ x_apples: '{0} apples' });
    expect(translateWithParameters('x_apples', 5)).toBe('5 apples');
  });

  it('should translate message with one parameter in the middle', () => {
    Initializer.setMessages({ x_apples: 'I have {0} apples' });
    expect(translateWithParameters('x_apples', 5)).toBe('I have 5 apples');
  });

  it('should translate message with one parameter in the end', () => {
    Initializer.setMessages({ x_apples: 'Apples: {0}' });
    expect(translateWithParameters('x_apples', 5)).toBe('Apples: 5');
  });

  it('should translate message with several parameters', () => {
    Initializer.setMessages({ x_apples: '{0}: I have {2} apples in my {1} baskets - {3}' });
    expect(translateWithParameters('x_apples', 1, 2, 3, 4)).toBe(
      '1: I have 3 apples in my 2 baskets - 4'
    );
  });

  it('should not be affected by replacement pattern XSS vulnerability of String.replace', () => {
    Initializer.setMessages({ x_apples: 'I have {0} apples' });
    expect(translateWithParameters('x_apples', '$`')).toBe('I have $` apples');
  });

  it('should not translate message but return its key', () => {
    expect(translateWithParameters('random', 5)).toBe('random.5');
    expect(translateWithParameters('random', 1, 2, 3)).toBe('random.1.2.3');
    expect(translateWithParameters('composite.random', 1, 2)).toBe('composite.random.1.2');
  });
});

describe('hasMessage', () => {
  it('should return that the message exists', () => {
    Initializer.setMessages({ foo: 'Foo', 'foo.bar': 'Foo Bar' });
    expect(hasMessage('foo')).toBe(true);
    expect(hasMessage('foo', 'bar')).toBe(true);
  });

  it('should return that the message is missing', () => {
    expect(hasMessage('foo')).toBe(false);
    expect(hasMessage('foo', 'bar')).toBe(false);
  });
});
