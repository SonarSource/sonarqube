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
  meta: {
    fixable: 'code',
  },
  create(context) {
    return {
      Identifier(node) {
        if (
          ['toHaveATooltipWithContent', 'toHaveNoA11yViolations'].includes(node.name) &&
          node.parent?.parent?.parent?.type !== 'AwaitExpression'
        ) {
          context.report({
            node: node.parent?.parent?.parent,
            message: `expect.${node.name}() is asynchronous; you must prefix expect() with await`,
            fix(fixer) {
              return fixer.insertTextBefore(node.parent?.parent?.parent, 'await ');
            },
          });
        }
      },
    };
  },
};
