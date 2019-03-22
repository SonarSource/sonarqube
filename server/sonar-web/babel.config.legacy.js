module.exports = {
  presets: [
    [
      '@babel/preset-env',
      {
        modules: false,
        targets: {
          browsers: [
            'last 3 Chrome versions',
            'last 3 Firefox versions',
            'last 3 Safari versions',
            'last 3 Edge versions',
            'IE 11'
          ]
        },
        corejs: 3,
        useBuiltIns: 'entry'
      }
    ],
    '@babel/preset-react'
  ],
  plugins: [
    '@babel/plugin-proposal-class-properties',
    ['@babel/plugin-proposal-object-rest-spread', { useBuiltIns: true }],
    'lodash'
  ],
  env: {
    production: {
      plugins: [
        '@babel/plugin-syntax-dynamic-import',
        '@babel/plugin-transform-react-constant-elements'
      ]
    },
    development: {
      plugins: [
        '@babel/plugin-syntax-dynamic-import',
        '@babel/plugin-transform-react-jsx-source',
        '@babel/plugin-transform-react-jsx-self'
      ]
    },
    test: {
      plugins: [
        '@babel/plugin-transform-modules-commonjs',
        'dynamic-import-node',
        '@babel/plugin-transform-react-jsx-source',
        '@babel/plugin-transform-react-jsx-self'
      ]
    }
  }
};
