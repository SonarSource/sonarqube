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
// @flow
/*:: import type { Event } from './types'; */
/*:: import type { State } from './components/ProjectActivityAppContainer'; */

export const addCustomEvent = (analysis /*: string */, event /*: Event */) => (
  state /*: State */
) => ({
  analyses: state.analyses.map(item => {
    if (item.key !== analysis) {
      return item;
    }
    return { ...item, events: [...item.events, event] };
  })
});

export const deleteEvent = (analysis /*: string */, event /*: string */) => (
  state /*: State */
) => ({
  analyses: state.analyses.map(item => {
    if (item.key !== analysis) {
      return item;
    }
    return {
      ...item,
      events: item.events.filter(eventItem => eventItem.key !== event)
    };
  })
});

export const changeEvent = (analysis /*: string */, event /*: Event */) => (
  state /*: State */
) => ({
  analyses: state.analyses.map(item => {
    if (item.key !== analysis) {
      return item;
    }
    return {
      ...item,
      events: item.events.map(
        eventItem => (eventItem.key === event.key ? { ...eventItem, ...event } : eventItem)
      )
    };
  })
});

export const deleteAnalysis = (analysis /*: string */) => (state /*: State */) => ({
  analyses: state.analyses.filter(item => item.key !== analysis)
});
