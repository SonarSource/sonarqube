/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Dict } from '../../../types/types';
import { LANGUAGES_CATEGORY } from './AdditionalCategoryKeys';

const CATEGORY_OVERRIDES: Dict<string> = {
  abap: LANGUAGES_CATEGORY,
  apex: LANGUAGES_CATEGORY,
  'c / c++ / objective-c': LANGUAGES_CATEGORY,
  'c#': LANGUAGES_CATEGORY,
  cloudformation: LANGUAGES_CATEGORY,
  cobol: LANGUAGES_CATEGORY,
  css: LANGUAGES_CATEGORY,
  flex: LANGUAGES_CATEGORY,
  go: LANGUAGES_CATEGORY,
  html: LANGUAGES_CATEGORY,
  java: LANGUAGES_CATEGORY,
  javascript: LANGUAGES_CATEGORY,
  'javascript / typescript': LANGUAGES_CATEGORY,
  json: LANGUAGES_CATEGORY,
  kotlin: LANGUAGES_CATEGORY,
  php: LANGUAGES_CATEGORY,
  'pl/i': LANGUAGES_CATEGORY,
  'pl/sql': LANGUAGES_CATEGORY,
  python: LANGUAGES_CATEGORY,
  rpg: LANGUAGES_CATEGORY,
  ruby: LANGUAGES_CATEGORY,
  scala: LANGUAGES_CATEGORY,
  swift: LANGUAGES_CATEGORY,
  't-sql': LANGUAGES_CATEGORY,
  terraform: LANGUAGES_CATEGORY,
  typescript: LANGUAGES_CATEGORY,
  'vb.net': LANGUAGES_CATEGORY,
  'visual basic': LANGUAGES_CATEGORY,
  xml: LANGUAGES_CATEGORY,
  yaml: LANGUAGES_CATEGORY
};

export default CATEGORY_OVERRIDES;
