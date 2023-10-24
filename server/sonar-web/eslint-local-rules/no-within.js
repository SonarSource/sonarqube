// For now lets enable it on this extension
// once we have made the refactoring we can add it to sonar-web and all other extension.
module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Prevent unsing within from testing library',
      category: 'Best Practices',
    },
    messages: {
      noWithin: "Don't use within as it may hide DOM update. Prefer using chain selector.",
    },
  },
  // eslint-disable-next-line object-shorthand
  create: function (context) {
    const fnNames = [];
    const currentFilePath = context.getFilename();
    const sourceCode = context.sourceCode;
    return {
      // eslint-disable-next-line object-shorthand
      CallExpression: function (node) {
        if (node.callee.name === 'within') {
          let scope = sourceCode.getScope(node);
          while (node.callee.name && scope && scope.set.get(node.callee.name) === undefined) {
            scope = scope.upper;
          }

          if (node.callee.name && scope) {
            let variable = scope.set.get(node.callee.name);
            if (variable.defs[0] && variable.defs[0].parent.source) {
              const importPath = variable.defs[0].parent.source.value;
              if (importPath.startsWith('@testing-library')) {
                context.report({
                  node: node.callee,
                  messageId: 'noWithin',
                });
              }
            }
          }
        }
      },
    };
  },
};
