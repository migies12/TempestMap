// __tests__/helpers.test.js

// Mock out AWS and Firebase Admin inside the test file
jest.mock('aws-sdk', () => {
    const DocumentClientMock = {
      scan: jest.fn(),
      delete: jest.fn(),
      put: jest.fn(),
      update: jest.fn(),
    };
  
    return {
      DynamoDB: {
        DocumentClient: jest.fn(() => DocumentClientMock),
      },
      config: {
        update: jest.fn(),
      },
    };
  });
  
  jest.mock('firebase-admin/app', () => ({
    initializeApp: jest.fn(() => ({})),
    applicationDefault: jest.fn(),
  }));
  
  jest.mock('firebase-admin/messaging', () => ({
    getMessaging: jest.fn(() => ({
      send: jest.fn(),
    })),
  }));
  
  // Now import the app file or the helper functions directly
  const AWS = require('aws-sdk');
  const { getMessaging } = require('firebase-admin/messaging');
  
  // Import the functions to test
  const {
    deleteAllEvents,
    appendEvents,
    dangerLevelCalc,
    notifyUsers,
  } = require('backend/tempest'); // <-- Adjust path accordingly
  
  describe('Helper Functions', () => {
    let mockDocumentClient;
    let mockSend;
    
    beforeAll(() => {
      mockDocumentClient = new AWS.DynamoDB.DocumentClient();
      mockSend = getMessaging().send;
    });
  
    beforeEach(() => {
      // Clear mocks before each test
      jest.clearAllMocks();
    });
  
    /* ------------------------------------------------------------------
     *  deleteAllEvents
     * ------------------------------------------------------------------ */
    describe('deleteAllEvents', () => {
      it('should scan the table and delete all items when items exist', async () => {
        // Mock the scan response to have some items
        mockDocumentClient.scan
          .mockResolvedValueOnce({
            Items: [{ event_id: 'abc123' }, { event_id: 'xyz789' }],
            LastEvaluatedKey: undefined,
          })
          .mockResolvedValueOnce({
            Items: [],
            LastEvaluatedKey: undefined,
          });
  
        // Call the function
        await deleteAllEvents();
  
        // Expect the calls
        expect(mockDocumentClient.scan).toHaveBeenCalledTimes(2); // one for items, one for no items
        expect(mockDocumentClient.delete).toHaveBeenCalledTimes(2);
  
        // The actual calls with the 'event_id' keys:
        expect(mockDocumentClient.delete).toHaveBeenCalledWith({
          TableName: 'event',
          Key: { event_id: 'abc123' },
        });
        expect(mockDocumentClient.delete).toHaveBeenCalledWith({
          TableName: 'event',
          Key: { event_id: 'xyz789' },
        });
      });
  
      it('should do nothing if no items are found', async () => {
        mockDocumentClient.scan.mockResolvedValue({
          Items: [],
          LastEvaluatedKey: undefined,
        });
  
        await deleteAllEvents();
  
        expect(mockDocumentClient.scan).toHaveBeenCalled();
        expect(mockDocumentClient.delete).not.toHaveBeenCalled();
      });
    });
  
    /* ------------------------------------------------------------------
     *  appendEvents
     * ------------------------------------------------------------------ */
    describe('appendEvents', () => {
      it('should call put for each event in the events array', async () => {
        const sampleEvents = [
          {
            event_id: '1',
            event_type: 'WF',
            event_name: 'Wildfire Sample',
            date: '2024-01-01',
            lat: 10,
            lng: 20,
            continent: 'NAR',
            country_code: 'US',
            created_time: '2024-01-01T12:00:00Z',
            source_event_id: 'source1',
            estimated_end_date: '2024-01-02',
          },
          {
            event_id: '2',
            event_type: 'EQ',
            event_name: 'Earthquake Sample',
            date: '2024-02-01',
            lat: 30,
            lng: 40,
            continent: 'NAR',
            country_code: 'US',
            created_time: '2024-02-01T12:00:00Z',
            source_event_id: 'source2',
            estimated_end_date: '2024-02-02',
          },
        ];
  
        // We don't need to mock the actual put if we just want to check it's being called
        mockDocumentClient.put.mockResolvedValue({});
  
        await appendEvents(sampleEvents);
  
        // Two events => two put calls
        expect(mockDocumentClient.put).toHaveBeenCalledTimes(2);
        // We check that it is called with the correct table name
        expect(mockDocumentClient.put).toHaveBeenNthCalledWith(1, expect.objectContaining({
          TableName: 'event',
        }));
        expect(mockDocumentClient.put).toHaveBeenNthCalledWith(2, expect.objectContaining({
          TableName: 'event',
        }));
      });
    });
  
    /* ------------------------------------------------------------------
     *  dangerLevelCalc
     * ------------------------------------------------------------------ */
    describe('dangerLevelCalc', () => {
      it('should return 100 when distance is 0 and event type is WF', () => {
        // lat/lon for the same point
        const lat1 = 10, lon1 = 10, lat2 = 10, lon2 = 10;
        const disasterType = 'WF'; // base danger is 100
  
        const level = dangerLevelCalc(lat1, lon1, lat2, lon2, disasterType);
        expect(level).toBe(100);
      });
  
      it('should return a reduced danger level when distance is large', () => {
        // Points are far, so expect a distance factor < 1
        const lat1 = 0, lon1 = 0;
        const lat2 = 10, lon2 = 10; // definitely some distance away
        const type = 'WF'; 
  
        const level = dangerLevelCalc(lat1, lon1, lat2, lon2, type);
  
        // We just make sure it's less than the base
        expect(level).toBeLessThan(100);
        expect(level).toBeGreaterThanOrEqual(0);
      });
  
      it('should handle unknown disasterType by returning undefined or NaN if not in baseDangerLevels', () => {
        const lat1 = 0, lon1 = 0;
        const lat2 = 0, lon2 = 0;
        const invalidType = 'XX';
  
        const level = dangerLevelCalc(lat1, lon1, lat2, lon2, invalidType);
        // The function as written will try to do "const danger = baseDangerLevels[disasterType]" 
        // which might be undefined => the final returned "scaledDanger" could be NaN.
        // So we check for NaN or something
        expect(level).toBeNaN();
      });
    });
  
    /* ------------------------------------------------------------------
     *  notifyUsers
     * ------------------------------------------------------------------ */
    describe('notifyUsers', () => {
      it('should send notifications to users who have notifications = true, and distance factor > 25', async () => {
        // We set up the mocks to simulate what scanning returns:
  
        // Scan of 'event' table
        mockDocumentClient.scan
          .mockResolvedValueOnce({
            Items: [
              // Danger will be 100 if user is in same location
              { event_id: 'event1', lat: 10, lng: 10, event_type: 'WF' },
            ],
            LastEvaluatedKey: undefined,
          })
          // Then the scan for 'user' table
          .mockResolvedValueOnce({
            Items: [
              { user_id: 'user1', notifications: true, latitude: 10, longitude: 10, regToken: 'token1' },
              { user_id: 'user2', notifications: false, latitude: 0, longitude: 0, regToken: 'token2' },
            ],
            LastEvaluatedKey: undefined,
          });
  
        // Call notifyUsers
        await notifyUsers();
  
        // user1 has notifications=true and is at the same spot => dangerLevel=100 => above 25 => should trigger
        // user2 has notifications=false => should not trigger
        expect(mockSend).toHaveBeenCalledTimes(1);
        expect(mockSend).toHaveBeenCalledWith(expect.objectContaining({
          token: 'token1',
          notification: expect.any(Object),
        }));
      });
  
      it('should not send any notifications if dangerLevel <= 25', async () => {
        // Suppose the user is far away => low danger
        mockDocumentClient.scan
          .mockResolvedValueOnce({
            Items: [
              { event_id: 'event1', lat: 10, lng: 10, event_type: 'WF' },
            ],
            LastEvaluatedKey: undefined,
          })
          .mockResolvedValueOnce({
            Items: [
              { user_id: 'user1', notifications: true, latitude: 50, longitude: 50, regToken: 'token1' },
            ],
            LastEvaluatedKey: undefined,
          });
  
        await notifyUsers();
  
        // This time, distance is large => dangerLevel will be well below base
        expect(mockSend).not.toHaveBeenCalled();
      });
  
      it('should skip users who have notifications = false', async () => {
        mockDocumentClient.scan
          .mockResolvedValueOnce({ // events
            Items: [
              { event_id: 'event2', lat: 10, lng: 10, event_type: 'WF' },
            ],
            LastEvaluatedKey: undefined,
          })
          .mockResolvedValueOnce({ // users
            Items: [
              { user_id: 'user2', notifications: false, latitude: 10, longitude: 10, regToken: 'token2' },
            ],
            LastEvaluatedKey: undefined,
          });
  
        await notifyUsers();
        expect(mockSend).not.toHaveBeenCalled();
      });
    });
  });
  