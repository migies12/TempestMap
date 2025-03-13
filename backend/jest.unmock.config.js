module.exports = {
    // Match tests in the "tests/mock" folder
    testMatch: ['**/tests/unmock/**/*.test.js'],
    // Specify a unique coverage directory
    collectCoverage: true,
    collectCoverageFrom: [
      '*.{js,jsx}',
      '!*.config.js'
    ],
    coverageDirectory: 'coverage/mock',
    // Other configuration options can go here
  };