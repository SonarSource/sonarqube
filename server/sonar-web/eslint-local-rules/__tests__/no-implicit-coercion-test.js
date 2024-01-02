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
const { RuleTester } = require('eslint');
const noImplicitCoercion = require('../no-implicit-coercion');

const ruleTester = new RuleTester({
  parserOptions: {
    ecmaFeatures: {
      jsx: true,
    },
  },
  parser: require.resolve('@typescript-eslint/parser'),
});

ruleTester.run('no-implicit-coercion', noImplicitCoercion, {
  valid: [
    {
      code: `
      function test(value?: number) {
        if (value === undefined) {
          return true;
        }
      }`,
    },
    {
      code: `
      function test(value?: number) {
        if (Boolean(value)) {
          return true;
        }
      }`,
    },
    {
      code: `
      function test(value?: {}) {
        if (!value) {
          return true;
        }
      }`,
    },
    {
      code: `
      function test(value: string) {
        if (value !== '') {
          return true;
        }
      }`,
    },
    {
      code: `
      function test(value?: number | {}) {
        return value !== undefined && value.toString();
      }`,
    },
    {
      code: `
      function test(value?: number) {
        return value ?? 100;
      }`,
    },
    {
      code: `
      interface Props {
        test?: number;
      }
      function Test(props: Props) {
        if (props.test !== undefined) {
          return props.test * 10;
        }
        return 100;
      }`,
    },
    {
      code: `
      interface Props {
        test?: number;
        check: boolean
      }
      function Test(props: Props) {
        if (props.check && props.test !== undefined) {
          return props.test * 10;
        }
        return 100;
      }`,
    },
    {
      code: `
      interface Props {
        test?: React.ReactNode;
      }
      function Test(props: Props) {
        if (props.test) {
          return props.test;
        }
        return null;
      }`,
    },
    {
      code: `
      interface Props {
        test?: number;
      }
      function Test(props: Props) {
        return (
          <div>
            {props.test !== undefined && <span>{props.test}</span>}
            {props.test === undefined && <span>100</span>}
          </div>
        );
      }`,
    },
  ],
  invalid: [
    {
      code: `
        function test(value?: number) {
          if (!value) {
            return true;
          }
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        function test(value?: number) {
          if (value) {
            return true;
          }
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        function test(value: string) {
          if (value) {
            return true;
          }
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        function test(value?: number) {
          return value && value > -1;
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        function test(value?: string | {}) {
          if (value) {
            return 1;
          }
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        function test(value?: number) {
          return value || 100;
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        interface Props {
          test?: number | {};
        }
        function Test(props: Props) {
          return props.test && props.test.toString();
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        interface Props {
          test?: number;
        }
        function Test(props: Props) {
          if (props.test) {
            return props.test * 10;
          }
          return 100;
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
        interface Props {
          test?: number;
          check: boolean
        }
        function Test(props: Props) {
          if (props.check && props.test) {
            return props.test * 10;
          }
          return 100;
        }`,
      errors: [{ messageId: 'noImplicitCoercion' }],
    },
    {
      code: `
      interface Props {
        test?: number;
      }
      function Test(props: Props) {
        return (
          <div>
            {props.test && <span>{props.test}</span>}
            {!props.test && <span>100</span>}
          </div>
        );
      }`,
      errors: [{ messageId: 'noImplicitCoercion' }, { messageId: 'noImplicitCoercion' }],
    },
  ],
});
