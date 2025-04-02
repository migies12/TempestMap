module.exports = {
  testEnvironment: 'node',
  // Match tests in the "tests/mock" folder (adjust the pattern if needed)
  testMatch: ['**/tests/mock/**/*.test.js'],
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
  // Specify a unique coverage directory
  coverageDirectory: 'coverage/mock',
  verbose: true,
  };