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
const { RuleTester } = require('eslint');
const noConditionalRenderingOfDeferredSpinner = require('../no-conditional-rendering-of-deferredspinner');

const ruleTester = new RuleTester({
  parser: require.resolve('@typescript-eslint/parser'),
  parserOptions: {
    ecmaFeatures: {
      jsx: true,
    },
  },
});

ruleTester.run(
  'no-conditional-rendering-of-deferredspinner',
  noConditionalRenderingOfDeferredSpinner,
  {
    valid: [
      {
        code: `function MyCompontent({ loading }) {
  return <>
    <DeferredSpinner loading={loading} />
  </>
}`,
      },
    ],
    invalid: [
      {
        code: `function MyCompontent({ loading }) {
  return <>
    {loading && <DeferredSpinner />}
  </>
}`,
        errors: [{ messageId: 'noConditionalRenderingOfDeferredSpinner' }],
      },
      {
        code: `function MyComponent({ loading }) {
  return <>
    {loading ? <DeferredSpinner /> : <div />}
  </>
}`,
        errors: [{ messageId: 'noConditionalRenderingOfDeferredSpinner' }],
      },
      {
        code: `function MyCompontent({ loaded }) {
  return <>
    {loaded ? <div /> : <DeferredSpinner />}
  </>
}`,
        errors: [{ messageId: 'noConditionalRenderingOfDeferredSpinner' }],
      },
    ],
  }
);
