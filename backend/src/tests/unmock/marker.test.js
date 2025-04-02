// AWS and Axios Mocks
jest.mock('aws-sdk', () => {
    const documentClientMocks = {
        scan: jest.fn().mockReturnThis(),
        put: jest.fn().mockReturnThis(),
        delete: jest.fn().mockReturnThis(),
        update: jest.fn().mockReturnThis(),
        get: jest.fn().mockReturnThis(),
        promise: jest.fn(),
    };
    return {
        config: { update: jest.fn() },
        DynamoDB: { DocumentClient: jest.fn(() => documentClientMocks) },
    };
});
jest.mock('axios');

const request = require('supertest');
const app = require('../../app');
const AWS = require('aws-sdk');

describe('UNMOCKED: Marker Route', () => {
    let docClient;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    /* --------------------------
     *  POST /user_marker
     * -------------------------- */
    describe('POST /user_marker', () => {
        it('should return 400 if required fields are missing', async () => {
            const response = await request(app).post('/user_marker').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty(
                'error',
                'Missing type, latitude, longitude, or description'
            );
        });

        it('should create a custom marker and return 201', async () => {
            docClient.put.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({}),
            });

            const markerData = {
                type: 'TestMarker',
                latitude: 40.7128,
                longitude: -74.006,
                description: 'Test Description',
            };

            const response = await request(app)
                .post('/user_marker')
                .send(markerData);

            expect(response.status).toBe(201);
            expect(response.body).toHaveProperty(
                'message',
                'User marker created successfully'
            );
            expect(response.body).toHaveProperty('user_marker');
            expect(docClient.put).toHaveBeenCalled();
        });
    });

    
    it('should return 200 and markers if the scan is successful', async () => {
      // Prepare a fake DynamoDB response
      const fakeItems = [
          {
              marker_id: '1',
              type: 'type1',
              latitude: 10.0,
              longitude: 20.0,
              description: 'A sample marker',
              comments: ['Nice place', 'Must visit'],
              created_at: '2025-04-01T00:00:00Z',
          },
      ];
      const fakeResult = { Items: fakeItems };

      // Mock the dynamoDB.scan method to resolve with fakeResult
      jest.spyOn(docClient, 'scan').mockReturnValue({
          promise: () => Promise.resolve(fakeResult),
      });

      // Make the GET request through the route
      const response = await request(app).get('/user_marker');

      // Assert that the response has a 200 status and a properly formatted marker list
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('markers');
      expect(response.body.markers).toEqual([
          {
              id: '1',
              type: 'type1',
              latitude: 10.0,
              longitude: 20.0,
              description: 'A sample marker',
              comments: ['Nice place', 'Must visit'],
              createdAt: '2025-04-01T00:00:00Z',
          },
      ]);
  });
});
