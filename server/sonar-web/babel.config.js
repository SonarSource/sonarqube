/*
 * Copyright (C) 2009-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

const envPlugins = [];

module.exports = {
  plugins: ['@emotion'],
  presets: [
    ['@babel/preset-react', { runtime: 'automatic' }],
    ['@babel/preset-typescript', { allowNamespaces: true }],
  ],
  env: {
    production: {
      plugins: envPlugins,
    },
    development: {
      plugins: envPlugins,
    },
  },
};
