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
      '!**/jobs/**',
      '!src/config/firebase.js'
    ],
    verbose: true,
  };