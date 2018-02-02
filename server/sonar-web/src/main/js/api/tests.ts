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
import throwGlobalError from '../app/utils/throwGlobalError';
import { Paging, TestCase, CoveredFile, BranchParameters } from '../app/types';
import { getJSON } from '../helpers/request';

export function getTests(
  parameters: {
    p?: number;
    ps?: number;
    sourceFileKey?: string;
    sourceFileLineNumber?: number;
    testFileKey: string;
    testId?: string;
  } & BranchParameters
): Promise<{ paging: Paging; tests: TestCase[] }> {
  return getJSON('/api/tests/list', parameters).catch(throwGlobalError);
}

export function getCoveredFiles(data: { testId: string }): Promise<CoveredFile[]> {
  return getJSON('/api/tests/covered_files', data).then(r => r.files, throwGlobalError);
}
