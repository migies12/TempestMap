const dangerLevelCalc = require('../../utils/dangerLevelCalc');

describe('dangerLevelCalc', () => {

  /**
   * Test: Returns full danger level when distance is zero.
   *
   * Inputs:
   * - Coordinates are the same (lat1 = 40, lon1 = -74, lat2 = 40, lon2 = -74).
   * - Disaster type: 'WF' (Wildfire) with a base danger level of 100.
   *
   * Expected Output:
   * - Since the distance is zero, the distance factor is 1.0.
   * - The calculated danger level should be Math.round(100 * 1.0) = 100.
   */
  test('should return full danger level when distance is zero', () => {
    const lat = 40;
    const lon = -74;
    const result = dangerLevelCalc(lat, lon, lat, lon, 'WF');
    expect(result).toBe(100);
  });

  /**
   * Test: Returns zero danger level when distance exceeds or equals 500,000 meters.
   *
   * Inputs:
   * - Coordinates are far apart (e.g., lat1 = 0, lon1 = 0 and lat2 = 10, lon2 = 10).
   * - Disaster type: 'EQ' (Earthquake) with a base danger level of 80.
   *
   * Expected Output:
   * - The calculated distance exceeds 500,000 meters, so the distance factor is 0.
   * - The calculated danger level should be Math.round(80 * 0) = 0.
   */
  test('should return zero danger level when distance exceeds 500,000 meters', () => {
    const result = dangerLevelCalc(0, 0, 10, 10, 'EQ');
    expect(result).toBe(0);
  });

  /**
   * Test: Returns an intermediate danger level for moderate distances.
   *
   * Inputs:
   * - Choose coordinates such that the distance is roughly 250,000 meters.
   *   (A difference of ~2.25 degrees in latitude approximates 250,000 meters.)
   *   For example: lat1 = 40, lon1 = -74, lat2 = 42.25, lon2 = -74.
   * - Disaster type: 'EQ' (Earthquake) with a base danger level of 80.
   *
   * Expected Output:
   * - The distance factor should be approximately 1.0 - (250,000/500,000) = 0.5.
   * - The calculated danger level should be Math.round(80 * 0.5) = 40.
   */
  test('should return intermediate danger level for moderate distances', () => {
    const result = dangerLevelCalc(40, -74, 42.25, -74, 'EQ');
    expect(result).toBe(40);
  });
});
