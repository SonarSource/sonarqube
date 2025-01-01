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

module.exports = {
  'use-jest-mocked': require('./use-jest-mocked'),
  'convert-class-to-function-component': require('./convert-class-to-function-component'),
  'no-conditional-rendering-of-spinner': require('./no-conditional-rendering-of-spinner'),
  'use-visibility-enum': require('./use-visibility-enum'),
  'use-componentqualifier-enum': require('./use-componentqualifier-enum'),
  'use-metrickey-enum': require('./use-metrickey-enum'),
  'use-metrictype-enum': require('./use-metrictype-enum'),
  'use-await-expect-async-matcher': require('./use-await-expect-async-matcher'),
  'no-implicit-coercion': require('./no-implicit-coercion'),
  'no-api-imports': require('./no-api-imports'),
  'no-within': require('./no-within'),
};
