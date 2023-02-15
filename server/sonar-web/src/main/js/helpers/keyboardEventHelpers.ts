/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
export function isShortcut(event: KeyboardEvent): boolean {
  return event.ctrlKey || event.metaKey;
}

export function isTextarea(
  event: KeyboardEvent
): event is KeyboardEvent & { target: HTMLTextAreaElement } {
  return event.target instanceof HTMLTextAreaElement;
}

export function isInput(
  event: KeyboardEvent
): event is KeyboardEvent & { target: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement } {
  return (
    event.target instanceof HTMLInputElement ||
    event.target instanceof HTMLSelectElement ||
    event.target instanceof HTMLTextAreaElement
  );
}

/*
 * Due to React 16 event delegation, stopPropagation called within react-day-picker is NOT preventing other event handlers from being called.
 * As a temporary workaround, we detect this special case using this utility function.
 * This utility function can be removed once we upgrade to React 17, since although there is still event delegation,
 * it is delegated up to the React root, which will stop propagation before it reaches document event handlers.
 */
export function isDatePicker(event: KeyboardEvent): boolean {
  return event.target instanceof Element && event.target.matches('.rdp-day');
}
