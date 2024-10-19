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
// no-import-from-specific-folder.js

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Warn against importing functions from a "api" folder',
      category: 'Best Practices',
    },
    messages: {
      noApiImports:
        'Check if an existing react-query retrieves this data. Use it instead of importing the API function directly.',
    },
  },
  create: function (context) {
    const fnNames = [];
    const currentFilePath = context.getFilename();

    if (
      ['queries', 'mocks', '__tests__'].some((path) => currentFilePath.split('/').includes(path))
    ) {
      return {};
    }

    return {
      ImportDeclaration: function (node) {
        const importPath = node.source.value;

        if (importPath.split('/').includes('api')) {
          fnNames.push(...node.specifiers.map((specifier) => specifier.local.name));
        }
      },
      CallExpression: function (node) {
        if (fnNames.includes(node.callee.name)) {
          context.report({
            node: node.callee,
            messageId: 'noApiImports',
          });
        }
      },
    };
  },
};
