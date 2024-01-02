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
import { shallow } from 'enzyme';
import React from 'react';
import Modal from '../../../components/controls/Modal';
import { mockEvent } from '../../../helpers/testUtils';
import KeyboardShortcutsModal from '../KeyboardShortcutsModal';

jest.mock('react', () => {
  let close: () => void;
  return {
    ...jest.requireActual('react'),
    useEffect: jest.fn().mockImplementation((f) => {
      close = f();
    }),
    clean: () => {
      close();
    },
  };
});

afterEach(() => {
  if ((React as any).clean as () => void) {
    (React as any).clean();
  }
});

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('hidden');

  document.dispatchEvent(new KeyboardEvent('keydown', { key: '?' }));

  expect(wrapper).toMatchSnapshot('visible');
});

it('should close correctly', () => {
  const wrapper = shallowRender();
  document.dispatchEvent(new KeyboardEvent('keydown', { key: '?' }));

  wrapper.find(Modal).props().onRequestClose!(mockEvent());

  expect(wrapper.type()).toBeNull();
});

it('should ignore other keydownes', () => {
  const wrapper = shallowRender();
  document.dispatchEvent(new KeyboardEvent('keydown', { key: '!' }));
  expect(wrapper.type()).toBeNull();
});

it.each([['input'], ['select'], ['textarea']])('should ignore events on a %s', (type) => {
  const wrapper = shallowRender();

  const fakeEvent = new KeyboardEvent('keydown', { key: '!' });

  Object.defineProperty(fakeEvent, 'target', {
    value: document.createElement(type),
  });

  document.dispatchEvent(fakeEvent);

  expect(wrapper.type()).toBeNull();
});

function shallowRender() {
  return shallow(<KeyboardShortcutsModal />);
}
