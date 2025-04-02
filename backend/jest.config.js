module.exports = {
    testEnvironment: 'node',
    collectCoverage: true,
    collectCoverageFrom: [
      '**/*.{js,jsx}',
      '!**/*.config.js',
      '!**/index.js',
      '!src/tempest.js',
      '!**/coverage/**',
      '!**/tests/**',
      '!**/jobs/**'
    ],
    coverageThreshold: {
      global: {
        branches: 100,
        functions: 100,
        lines: 100,
        statements: 100
      }
    },
    verbose: true,
  };