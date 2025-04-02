const { fetchDisasterData } = require('../../services/weatherService');
const axios = require('axios');
const dynamoDB = require('../../config/aws');

// Set a dummy API key for header construction.
process.env.AMBEE_API_KEY = 'dummy-api-key';

// AWS and Axios Mocks
jest.mock('axios');
jest.mock('../../config/aws');

describe('fetchDisasterData failure scenarios', () => {

  let consoleErrorSpy;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });
  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  /**
   * Test: Failure during the API data fetch.
   *
   * Inputs:
   * - The axios.get call for one of the API endpoints (e.g., default events) is simulated to reject with an error.
   *
   * Expected Behavior:
   * - The Promise.all that fetches the two API responses fails.
   * - The error is caught in the try/catch block in fetchDisasterData.
   * - The error is logged with the prefix "Error fetching weather/disaster data:".
   * - The function rethrows the error.
   *
   * Expected Outputs:
   * - fetchDisasterData should reject with the simulated error.
   * - console.error is called with the appropriate error message.
   */
  test('should throw error when axios.get fails', async () => {

    axios.get.mockRejectedValue(new Error('API failure'));

    await expect(fetchDisasterData()).rejects.toThrow('API failure');

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error fetching weather/disaster data:',
      expect.any(Error)
    );

    expect(dynamoDB.scan).not.toHaveBeenCalled();
    expect(dynamoDB.put).not.toHaveBeenCalled();
  });

  /**
   * Test: Should log error when deletion of events fails.
   *
   * Inputs:
   * - Both axios.get calls return valid data (simulated disaster events) so that combinedEvents is non-empty.
   * - dynamoDB.scan (called in deleteAllEvents) returns an Items array with one item, triggering deletion.
   * - dynamoDB.delete (called for each item in Items) is simulated to reject with an error.
   * - dynamoDB.put (called in appendEvents) resolves successfully.
   *
   * Expected Behavior:
   * - deleteAllEvents attempts to delete existing events.
   * - The deletion promise for the existing event fails, which is caught and logged as:
   *   "Error deleting events:" followed by the error.
   * - Despite the deletion error, fetchDisasterData continues to append the new events.
   *
   * Expected Outputs:
   * - console.error should be called with the deletion error message.
   */
  test('should log error when deletion of events fails', async () => {

    axios.get.mockImplementation((url, config) => {

      return Promise.resolve({
        data: { 
          result: [
            {
              event_type: 'storm',
              event_name: 'Storm Event',
              date: '2025-04-02',
              lat: 35,
              lng: -85,
              continent: 'NA',
              country_code: 'US',
              created_time: '2025-04-02T13:00:00Z',
              source_event_id: 'storm123',
              estimated_end_date: '2025-04-03T13:00:00Z',
            },
          ]
        },
      });
    });

    // --- Simulate deletion error ---
    dynamoDB.scan.mockReturnValue({
      promise: jest.fn().mockResolvedValue({ Items: [{ event_id: 'existing-event-1' }] }),
    });

    // For deletion, simulate a failure.
    dynamoDB.delete.mockReturnValue({
      promise: jest.fn().mockRejectedValue(new Error('Deletion failed')),
    });
    // For appendEvents, simulate a successful put.
    dynamoDB.put.mockReturnValue({
      promise: jest.fn().mockResolvedValue({}),
    });

    await fetchDisasterData();

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error deleting events:',
      expect.any(Error)
    );
  });

  /**
   * Test: Should log error when inserting an event fails.
   *
   * Inputs:
   * - Both axios.get calls return valid disaster data resulting in combined events.
   * - dynamoDB.scan (used in deleteAllEvents) returns an empty Items array, so no deletion occurs.
   * - In appendEvents, dynamoDB.put is simulated to reject for an event, triggering an insertion error.
   *
   * Expected Behavior:
   * - deleteAllEvents logs that no events were found to delete.
   * - appendEvents attempts to insert events.
   * - The put operation fails for an event, and the error is caught and logged as:
   *   "Error inserting event [event.event_id]:" followed by the error.
   *
   * Expected Outputs:
   * - console.error should be called with the insertion error message.
   */
  test('should log error when inserting an event fails', async () => {

    axios.get.mockImplementation((url, config) => {
      return Promise.resolve({
        data: {
          result: [
            {
              event_type: 'storm',
              event_id: 'input-event-1',
              event_name: 'Storm Event',
              date: '2025-04-02',
              lat: 35,
              lng: -85,
              continent: 'NA',
              country_code: 'US',
              created_time: '2025-04-02T13:00:00Z',
              source_event_id: 'storm123',
              estimated_end_date: '2025-04-03T13:00:00Z',
            },
          ],
        },
      });
    });

    // --- Simulate that there are no existing events to delete ---
    dynamoDB.scan.mockReturnValue({
      promise: jest.fn().mockResolvedValue({ Items: [] }),
    });

    // --- Simulate insertion error in appendEvents ---
    dynamoDB.put.mockReturnValue({
      promise: jest.fn().mockRejectedValue(new Error('Insertion failed')),
    });

    await fetchDisasterData();

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      expect.stringContaining('Error inserting event'),
      expect.any(Error)
    );
  });
});
