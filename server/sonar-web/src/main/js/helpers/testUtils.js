/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
export const mockEvent = {
  target: { blur() {} },
  currentTarget: { blur() {} },
  preventDefault() {},
  stopPropagation() {}
};

export const click = (element, event = {}) => element.simulate('click', { ...mockEvent, ...event });

export const clickOutside = (event = {}) => {
  const dispatchedEvent = new MouseEvent('click', event);
  window.dispatchEvent(dispatchedEvent);
};

export const submit = element =>
  element.simulate('submit', {
    preventDefault() {}
  });

export const change = (element, value) =>
  element.simulate('change', {
    target: { value },
    currentTarget: { value }
  });

export const keydown = keyCode => {
  const event = new KeyboardEvent('keydown', { keyCode });
  document.dispatchEvent(event);
};

export const elementKeydown = (element, keyCode) => {
  element.simulate('keydown', {
    currentTarget: { element },
    keyCode,
    preventDefault() {}
  });
};
