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
  const axios = require('axios');
  
  describe('UNMOCKED: Index Route', () => {
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
     *  GET /
     * -------------------------- */
    describe('GET /', () => {
      it('200 with a success message', async () => {
        const response = await request(app).get('/');
        expect(response.status).toBe(200);
        expect(response.body).toEqual({ message: 'Success' });
      });
    });
  
    /* --------------------------
     *  POST /test_cron
     * -------------------------- */
    describe('POST /test_cron', () => {
      it('should call fetchDisasterData and return 200', async () => {
        // Resolve axios calls for default and WF disasters
        axios.get.mockResolvedValueOnce({ data: { result: [] } });
        axios.get.mockResolvedValueOnce({ data: { result: [] } });
  
        docClient.scan.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({ Items: [], LastEvaluatedKey: null }),
        });
  
        const response = await request(app).post('/test_cron');
  
        expect(response.status).toBe(200);
        expect(response.body).toEqual({
          message: 'Disaster data fetched successfully!',
        });
        expect(axios.get).toHaveBeenCalled();
      });
    });
  });
  