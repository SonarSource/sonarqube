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
    messages: {
      noConditionalRenderingOfDeferredSpinner:
        'For accessibility reasons, you should not conditionally render a <Spinner />. Always render it, and pass a loading prop instead.',
    },
  },
  create(context) {
    return {
      JSXExpressionContainer(node) {
        switch (node.expression.type) {
          case 'LogicalExpression':
            const { right } = node.expression;
            if (isDeferredSpinnerComponent(right)) {
              context.report({ node, messageId: 'noConditionalRenderingOfDeferredSpinner' });
            }
            break;

          case 'ConditionalExpression':
            const { consequent, alternate } = node.expression;
            if (isDeferredSpinnerComponent(consequent)) {
              context.report({ node, messageId: 'noConditionalRenderingOfDeferredSpinner' });
            }
            if (isDeferredSpinnerComponent(alternate)) {
              context.report({ node, messageId: 'noConditionalRenderingOfDeferredSpinner' });
            }
            break;
        }
      },
    };
  },
};

function isDeferredSpinnerComponent(element) {
  return (
    element.type === 'JSXElement' &&
    element.openingElement &&
    element.openingElement.name.name === 'Spinner'
  );
}
