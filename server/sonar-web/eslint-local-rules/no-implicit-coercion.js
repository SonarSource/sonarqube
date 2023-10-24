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
module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      description:
        'Enforce using explicit comparison instead of implicit coercion for certain variable types',
      category: 'Best Practices',
      recommended: true,
    },
    messages: {
      noImplicitCoercion:
        'Use explicit comparison instead of implicit coercion for strings and numbers.',
    },
  },
  create(context) {
    return {
      UnaryExpression: (node) => {
        const { argument, operator } = node;

        if (operator === '!') {
          checkImplicitCoercion(context, argument);
        }
      },
      LogicalExpression: (node) => {
        const { left, operator } = node;
        if (operator === '??') {
          return;
        }
        if (isVariableOrObjectField(left)) {
          checkImplicitCoercion(context, left);
        }
      },
      IfStatement: (node) => {
        const { test } = node;
        checkImplicitCoercion(context, test);
      },
    };
  },
};

const isForbiddenType = (type) =>
  type.intrinsicName === 'number' || type.intrinsicName === 'string';

const isVariableOrObjectField = (node) =>
  node.type === 'Identifier' || node.type === 'MemberExpression';

function checkImplicitCoercion(context, argument) {
  const tsNodeMap = context.parserServices.esTreeNodeToTSNodeMap;
  const typeChecker = context.parserServices?.program?.getTypeChecker();
  const type = typeChecker.getTypeAtLocation(tsNodeMap.get(argument));
  if (type.aliasSymbol && type.aliasSymbol.name === 'ReactNode') {
    return;
  }
  if (type.isUnion() ? type.types.some(isForbiddenType) : isForbiddenType(type)) {
    context.report({
      node: argument,
      messageId: 'noImplicitCoercion',
    });
  }
}
