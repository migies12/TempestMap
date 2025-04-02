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
  
  describe('UNMOCKED: Users Route', () => {
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
     *  POST /user
     * -------------------------- */
    describe('POST /user', () => {
      it('create a user and return 200', async () => {
        docClient.put.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({}),
        });
  
        const userData = {
          user_id: 'xyz',
          name: 'Test User',
          location: 'Testville',
          latitude: 12.34,
          longitude: 56.78,
          account_type: 'basic',
          email: 'test@example.com',
          regToken: 'abc123',
          notifications: true,
        };
  
        const response = await request(app).post('/user').send(userData);
  
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty(
          'message',
          'User created successfully'
        );
        expect(docClient.put).toHaveBeenCalled();
      });

      it('should create a user (without id) and return 200', async () => {
        docClient.put.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({}),
        });
  
        const userData = {
          name: 'Test User',
          location: 'Testville',
          latitude: 12.34,
          longitude: 56.78,
          account_type: 'basic',
          email: 'test@example.com',
          regToken: 'abc123',
          notifications: true,
        };
  
        const response = await request(app).post('/user').send(userData);
  
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty(
          'message',
          'User created successfully'
        );
        expect(docClient.put).toHaveBeenCalled();
      });


    });
  
    /* --------------------------
     *  GET /user/:user_id
     * -------------------------- */
    describe('GET /user/:user_id', () => {
      it('should retrieve a user', async () => {
        docClient.get.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({
            Item: { user_id: 'xyz', name: 'Test User' },
          }),
        });
  
        const response = await request(app).get('/user/xyz');
        expect(response.status).toBe(200);
        expect(response.body).toEqual({
          user: { user_id: 'xyz', name: 'Test User' },
        });
      });
    });
  
    /* --------------------------
     *  POST /user/locations
     * -------------------------- */
    describe('POST /user/locations', () => {
      it('should update user location', async () => {
        docClient.update.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({
            Attributes: { latitude: 12.34, longitude: 56.78 },
          }),
        });
  
        const response = await request(app)
          .post('/user/locations')
          .send({ user_id: 'xyz', latitude: 12.34, longitude: 56.78 });
  
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty(
          'message',
          'User location updated successfully'
        );
        expect(docClient.update).toHaveBeenCalled();
      });
    });
  });
  