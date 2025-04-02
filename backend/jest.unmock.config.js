module.exports = {
    // Match tests in the "tests/mock" folder
    testMatch: ['**/tests/unmock/**/*.test.js'],
    // Specify a unique coverage directory
    collectCoverage: true,
    // Collect coverage only from source files, excluding config and index files if needed
    collectCoverageFrom: [
      '**/*.{js,jsx}',
      '!**/*.config.js',
      '!**/index.js',
      '!tempest.js',
      '!**/coverage/**',
      '!**/tests/**',
      '!**/jobs/**'
    ],
    coverageDirectory: 'coverage/mock',
    // Other configuration options can go here
  };