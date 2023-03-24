module.exports = {
  testRegex: '(/__tests__/.*|\\-test)\\.(t|j)s$',
  transform: {
    '^.+\\.(t|j)s$': [
      '@swc/jest',
      {
        jsc: {
          target: 'es2018',
        },
      },
    ],
  },
};
