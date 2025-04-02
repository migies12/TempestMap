const { fetchDisasterData } = require('../../services/weatherService');
const axios = require('axios');
const dynamoDB = require('../../config/aws');

// Set a dummy API key so that headers are built properly.
process.env.AMBEE_API_KEY = 'dummy-api-key';

// AWS and Axios Mocks
jest.mock('axios');
jest.mock('../../config/aws');

describe('fetchDisasterData success scenario', () => {

    let consoleLogSpy;

    beforeEach(() => {
        jest.clearAllMocks();
        consoleLogSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    afterEach(() => {
        consoleLogSpy.mockRestore();
    });

    /**
     * Test: Successful weather/disaster data fetch and processing.
     *
     * Inputs:
     * - Two axios.get calls: one for default events and one for wildfire events.
     *   - The first call (default) returns one event (a storm event).
     *   - The second call (wildfire events) returns one event (a wildfire event).
     * - DynamoDB.scan (called by deleteAllEvents) returns no events to delete.
     * - DynamoDB.put (called by appendEvents) resolves successfully for each event.
     *
     * Expected Behavior:
     * - Both axios.get calls are executed successfully.
     * - deleteAllEvents is called and finds no existing events (logging "No events found to delete.").
     * - The events from both API responses are combined (total of 2 events).
     * - For each event in the combined data, a call to dynamoDB.put is made.
     * - No errors are thrown.
     *
     * Expected Outputs:
     * - Two calls to axios.get.
     * - One call to dynamoDB.scan.
     * - Two calls to dynamoDB.put (one per event in the combined result).
     */

    test('should fetch disaster data, delete old events, and append new events successfully', async () => {
       
        axios.get.mockImplementation((url, config) => {
            if (config.params && config.params.eventType === 'WF') {
                // For the wildfire events call.
                return Promise.resolve({
                    data: {
                        result: [
                            {
                                event_type: 'wildfire',
                                event_name: 'Wildfire Event',
                                date: '2025-04-02',
                                lat: 40,
                                lng: -80,
                                continent: 'NA',
                                country_code: 'US',
                                created_time: '2025-04-02T12:00:00Z',
                                source_event_id: 'wf123',
                                estimated_end_date: '2025-04-03T12:00:00Z',
                            },
                        ],
                    },
                });
            } else {
                
                // For the default events call.
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
                        ],
                    },
                });
            }
        });

        // --- Mocking dynamoDB.scan (used in deleteAllEvents) ---
        dynamoDB.scan.mockReturnValue({
            promise: jest.fn().mockResolvedValue({ Items: [] }),
        });

        // --- Mocking dynamoDB.put (used in appendEvents) ---
        dynamoDB.put.mockReturnValue({
            promise: jest.fn().mockResolvedValue({}),
        });

        await fetchDisasterData();

        // Assertions on axios calls.
        expect(axios.get).toHaveBeenCalledTimes(2);

        expect(dynamoDB.scan).toHaveBeenCalledTimes(1);

        // The combined events from both API responses should be 2.
        expect(dynamoDB.put).toHaveBeenCalledTimes(2);

        // Optionally, verify that the combined events were logged.
        expect(consoleLogSpy).toHaveBeenCalledWith(
            'Combined weather/disaster data retrieved:',
            expect.arrayContaining([
                expect.objectContaining({ event_type: 'storm' }),
                expect.objectContaining({ event_type: 'wildfire' }),
            ])
        );
    });
});
