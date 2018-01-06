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
/*:: import type { Measure, MeasureEnhanced } from '../../components/measure/types'; */

/*:: type ComponentIntern = {
  isFavorite?: boolean,
  isRecentlyBrowsed?: boolean,
  key: string,
  match?: string,
  name: string,
  organization?: string,
  project?: string,
  qualifier: string
}; */

/*:: export type Component = ComponentIntern & { measures?: Array<Measure> }; */

/*:: export type ComponentEnhanced = ComponentIntern & {
  value?: ?string,
  leak?: ?string,
  measures: Array<MeasureEnhanced>
}; */

/*:: export type Paging = {
  pageIndex: number,
  pageSize: number,
  total: number
}; */

/*:: export type Period = {
  index: number,
  date: string,
  mode: string,
  parameter?: string
}; */

/*:: export type Query = {
  metric: ?string,
  selected: ?string,
  view: string
}; */
