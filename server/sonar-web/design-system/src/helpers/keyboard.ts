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
export enum Key {
  ArrowLeft = 'ArrowLeft',
  ArrowUp = 'ArrowUp',
  ArrowRight = 'ArrowRight',
  ArrowDown = 'ArrowDown',

  Alt = 'Alt',
  Option = 'Option',
  Backspace = 'Backspace',
  CapsLock = 'CapsLock',
  Meta = 'Meta',
  Control = 'Control',
  Command = 'Command',
  Delete = 'Delete',
  End = 'End',
  Enter = 'Enter',
  Escape = 'Escape',
  Home = 'Home',
  PageDown = 'PageDown',
  PageUp = 'PageUp',
  Shift = 'Shift',
  Space = ' ',
  Tab = 'Tab',
  Click = 'Click',
}

export function isShortcut(event: KeyboardEvent): boolean {
  return event.ctrlKey || event.metaKey;
}

const INPUT_TAGS = ['INPUT', 'SELECT', 'TEXTAREA', 'UBCOMMENT'];

export function isInput(event: KeyboardEvent): boolean {
  const { tagName } = event.target as HTMLElement;
  return INPUT_TAGS.includes(tagName);
}

export function isTextarea(
  event: KeyboardEvent,
): event is KeyboardEvent & { target: HTMLTextAreaElement } {
  return event.target instanceof HTMLTextAreaElement;
}
