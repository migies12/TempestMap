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
  
  describe('UNMOCKED: Events Route', () => {
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
     *  GET /event
     * -------------------------- */
    describe('GET /event', () => {
      it('should return 200 and the events', async () => {
        docClient.scan.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({
            Items: [{ event_id: '123', event_name: 'Test Event' }],
            LastEvaluatedKey: null,
          }),
        });
  
        const response = await request(app).get('/event');
        expect(response.status).toBe(200);
        expect(response.body).toEqual({
          events: [{ event_id: '123', event_name: 'Test Event' }],
        });
      });
    });
  
    /* --------------------------
     *  GET /event/firms
     * -------------------------- */
    describe('GET /event/firms', () => {
      it('should return 200 and JSON from CSV', async () => {
        const mockedStream = {
          pipe: () => mockedStream,
          on: (event, handler) => {
            if (event === 'data') {
              handler({ testField: 'value' }); // Simulate one CSV row
            }
            if (event === 'end') {
              handler();
            }
            return mockedStream;
          },
        };
  
        axios.get.mockResolvedValueOnce({ data: mockedStream });
        const response = await request(app).get('/event/firms');
        expect(response.status).toBe(200);
        expect(Array.isArray(response.body)).toBe(true);
        expect(response.body).toMatchObject([{ testField: 'value' }]);
      });
    });
  });
  