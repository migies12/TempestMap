// AWS Mock
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
    DynamoDB: {
      DocumentClient: jest.fn(() => documentClientMocks),
    },
  };
});

// Axios Mock
jest.mock('axios');

const request = require('supertest');
// Updated app import for the refactored project structure
const app = require('../../app');
const AWS = require('aws-sdk');
const axios = require('axios');

describe('Backend Tests (Success)', () => {
  let docClient;
  let consoleErrorSpy;

  beforeAll(() => {
    docClient = new AWS.DynamoDB.DocumentClient();
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
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
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // for default disasters
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // for WF disasters

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
   *  POST /user_marker (formerly /event/custom)
   * -------------------------- */
  describe('POST /user_marker', () => {
    it('should return 400 if required fields are missing', async () => {
      const response = await request(app).post('/user_marker').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing type, latitude, longitude, or description');
    });

    it('should create a custom marker and return 201', async () => {
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({}),
      });

      const markerData = {
        type: 'TestMarker',
        latitude: 40.7128,
        longitude: -74.0060,
        description: 'Test Description'
      };

      const response = await request(app)
        .post('/user_marker')
        .send(markerData);

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty('message', 'User marker created successfully');
      expect(response.body).toHaveProperty('user_marker');
      expect(docClient.put).toHaveBeenCalled();
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

  /* --------------------------
   *  POST /comment/:id
   * -------------------------- */
  describe('POST /comment/:id', () => {
    it('should return 400 if missing required parameters', async () => {
      const response = await request(app).post('/comment/123').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing required parameters');
    });

    it('should append a comment and return 200', async () => {
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } }),
      });

      const response = await request(app)
        .post('/comment/123')
        .send({ comment: 'Nice event', user: 'John', type: 'event' });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('message', 'Comment appended successfully');
      expect(docClient.update).toHaveBeenCalled();
    });
  });

  /* --------------------------
   *  GET /comment/:event_id
   * -------------------------- */
  describe('GET /comment/:event_id', () => {
    it('should retrieve comments for the event', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ text: 'Test comment' }] },
        }),
      });

      const response = await request(app).get('/comment/123');
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('comments');
      expect(response.body.comments[0].text).toBe('Test comment');
    });
  });

  /* --------------------------
   *  DELETE /comment/:event_id
   * -------------------------- */
  describe('DELETE /comment/:event_id', () => {
    it('should delete a comment', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc123', text: 'Hi' }] },
        }),
      });

      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } }),
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'abc123' });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('message', 'Comment removed successfully.');
      expect(docClient.get).toHaveBeenCalled();
      expect(docClient.update).toHaveBeenCalled();
    });
  });

  /* --------------------------
   *  POST /user
   * -------------------------- */
  describe('POST /user', () => {
    it('should create a user and return 201', async () => {
      docClient.put.mockReturnValueOnce({ promise: jest.fn().mockResolvedValue({}) });

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

      const response = await request(app)
        .post('/user')
        .send(userData);

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty('message', 'User created successfully');
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
      expect(response.body).toHaveProperty('message', 'User location updated successfully');
      expect(docClient.update).toHaveBeenCalled();
    });
  });
});
