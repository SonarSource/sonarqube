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
import { mockQualityProfile } from '../../../helpers/testMocks';
import { QP_1, QP_2, QP_2_Parent, QP_3, QP_4, QP_5, QP_6 } from './ids';

export function mockQualityProfilesList() {
  return [
    mockQualityProfile({
      key: QP_1,
      name: 'QP Foo',
      language: 'java',
      languageName: 'Java',
      actions: { edit: true },
    }),
    mockQualityProfile({
      key: QP_2_Parent,
      name: 'QP Bar Parent',
      language: 'py',
      languageName: 'Python',
    }),
    mockQualityProfile({
      key: QP_2,
      name: 'QP Bar',
      language: 'py',
      languageName: 'Python',
      isInherited: true,
      parentKey: QP_2_Parent,
      parentName: 'QP Bar Parent',
    }),
    mockQualityProfile({ key: QP_3, name: 'QP FooBar', language: 'java', languageName: 'Java' }),
    mockQualityProfile({
      key: QP_4,
      name: 'QP FooBarBaz',
      language: 'java',
      languageName: 'Java',
    }),
    mockQualityProfile({
      key: QP_5,
      name: 'QP FooBaz',
      language: 'java',
      languageName: 'Java',
      isInherited: true,
      parentKey: QP_4,
      parentName: 'QP FooBarBaz',
    }),
    mockQualityProfile({
      key: QP_6,
      isBuiltIn: true,
      name: 'QP Qux',
      language: 'java',
      languageName: 'Java',
    }),
  ];
}
