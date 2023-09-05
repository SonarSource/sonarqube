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
