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
    messages: {
      noConditionalRenderingOfDeferredSpinner:
        'For accessibility reasons, you should not conditionally render a <DeferredSpinner />. Always render it, and pass a loading prop instead.',
    },
  },
  create(context) {
    return {
      JSXExpressionContainer(node) {
        if (
          node.expression.type === 'LogicalExpression' &&
          node.expression.right.type === 'JSXElement' &&
          node.expression.right.openingElement.name.name === 'DeferredSpinner'
        ) {
          context.report({ node, messageId: 'noConditionalRenderingOfDeferredSpinner' });
        }
      },
    };
  },
};
