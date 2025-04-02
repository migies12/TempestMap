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
  });
  