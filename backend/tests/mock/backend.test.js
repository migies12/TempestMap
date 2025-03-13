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
const app = require('backend/tempest');
const AWS = require('aws-sdk');
const axios = require('axios');

describe('Backend Tests', () => {
    let docClient;
    let consoleErrorSpy;
  
    beforeAll(() => {
      docClient = new AWS.DynamoDB.DocumentClient();
      consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    });
  
    beforeEach(() => {
      // Reset all the mocks before each test
      jest.clearAllMocks();
    });

  /* --------------------------
   *  GET /
   * -------------------------- */
  describe('GET /', () => {
    it('should return 200 with a success message', async () => {
      const response = await request(app).get('/');
      expect(response.status).toBe(200);
      expect(response.body).toEqual({ message: "Success" });
    });
  });

  /* --------------------------
   *  POST /test_cron
   * -------------------------- */
  describe('POST /test_cron', () => {
    it('should call fetchDisasterData and return 200', async () => {
      // We know fetchDisasterData calls axios.get under the hood, so let's mock it:
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // For "default"
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // For "WF"

      // Because fetchDisasterData also calls deleteAllEvents, which calls scan + delete,
      // let's mock that chain. Typically, it scans for items, so we mock out the result:
      docClient.scan.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Items: [], LastEvaluatedKey: null }),
      });

      // Make the request
      const response = await request(app).post('/test_cron');

      expect(response.status).toBe(200);
      expect(response.body).toEqual({ message: "Disaster data fetched successfully!" });

      // Verify axios was called at least once
      expect(axios.get).toHaveBeenCalled();
    });

    it('should return 500 if an error occurs', async () => {
      // Force an error in axios
      axios.get.mockRejectedValue(new Error('Some fetch error'));

      const response = await request(app).post('/test_cron');
      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error fetching disaster data');
    });
  });

  /* --------------------------
   *  GET /event
   * -------------------------- */
  describe('GET /event', () => {
    it('should return 200 and the events', async () => {
      // Mock the scan results from DynamoDB
      docClient.scan.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Items: [{ event_id: '123', event_name: 'Test Event' }],
          LastEvaluatedKey: null
        })
      });

      const response = await request(app).get('/event');
      expect(response.status).toBe(200);
      expect(response.body).toEqual({
        events: [{ event_id: '123', event_name: 'Test Event' }]
      });
    });

    it('should return 500 if an error occurs', async () => {
      docClient.scan.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error'))
      });

      const response = await request(app).get('/event');
      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error fetching events');
    });
  });

  /* --------------------------
   *  POST /event/custom
   * -------------------------- */
  describe('POST /event/custom', () => {
    it('should create a custom event and return 201', async () => {
      // Mock the put call
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({})
      });

      const response = await request(app)
        .post('/event/custom')
        .send({
          latitude: 40.7128,
          longitude: -74.0060,
          markerType: 'TestMarker'
        });

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty('message', 'Custom event created successfully');
      expect(response.body).toHaveProperty('event');
      expect(docClient.put).toHaveBeenCalled();
    });

    it('should return 400 if required fields are missing', async () => {
      const response = await request(app).post('/event/custom').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if DynamoDB put fails', async () => {
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Put Error'))
      });

      const response = await request(app)
        .post('/event/custom')
        .send({
          latitude: 40.7128,
          longitude: -74.0060,
          markerType: 'TestMarker'
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error creating custom event');
    });
  });

  /* --------------------------
   *  GET /event/firms
   * -------------------------- */
  describe('GET /event/firms', () => {
    it('should return 200 and JSON from CSV', async () => {
      // We can simulate a stream by using Node’s built-in stream libraries or mocking the `.pipe(csv())`
      // For simplicity, let's do a minimal mock where we manually call the `.on` callbacks.
      const mockedStream = {
        pipe: () => mockedStream,
        on: (event, handler) => {
          if (event === 'data') {
            handler({ testField: 'value' }); // One row
          }
          if (event === 'end') {
            handler();
          }
          return mockedStream;
        }
      };

      axios.get.mockResolvedValueOnce({ data: mockedStream });
      const response = await request(app).get('/event/firms');
      expect(response.status).toBe(200);
      // The result should be the array of parsed CSV rows
      expect(Array.isArray(response.body)).toBe(true);
      expect(response.body).toMatchObject([{ testField: 'value' }]);
    });

    it('should return 500 if an error occurs fetching data', async () => {
      axios.get.mockRejectedValue(new Error('FIRMS fetch error'));

      const response = await request(app).get('/event/firms');
      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Failed to fetch FIRMS data');
    });
  });

  /* --------------------------
   *  POST /comment/:event_id
   * -------------------------- */
  describe('POST /comment/:event_id', () => {
    it('should append a comment and return 200', async () => {
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } })
      });

      const response = await request(app)
        .post('/comment/123')
        .send({ comment: 'Nice event', user: 'John' });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('message', 'Comment appended successfully');
      expect(docClient.update).toHaveBeenCalled();
    });

    it('should return 400 if missing event_id or comment', async () => {
      // No comment in body
      const response = await request(app).post('/comment/123').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing event_id or comment in request body');
    });

    it('should return 500 if DynamoDB update fails', async () => {
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error'))
      });

      const response = await request(app)
        .post('/comment/123')
        .send({ comment: 'Hello' });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error appending comment');
    });
  });

  /* --------------------------
   *  GET /comment/:event_id
   * -------------------------- */
  describe('GET /comment/:event_id', () => {
    it('should retrieve comments for the event', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Item: { comments: [{ text: 'Test comment' }] } })
      });

      const response = await request(app).get('/comment/123');
      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('comments');
      expect(response.body.comments[0].text).toBe('Test comment');
    });

    it('should return 400 if event_id is missing', async () => {
      const response = await request(app).get('/comment/');
      // The route itself expects `/comment/:event_id`, so this is invalid.
      // In typical Express usage this might 404, but you can adapt the test to your structure.
      expect(response.status).toBeGreaterThanOrEqual(400);
    });

    it('should return 404 if no event found', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({})
      });

      const response = await request(app).get('/comment/123');
      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Event not found.');
    });

    it('should return 500 if DynamoDB get fails', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB get error'))
      });

      const response = await request(app).get('/comment/123');
      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error retrieving comments.');
    });
  });

  /* --------------------------
   *  DELETE /comment/:event_id
   * -------------------------- */
  describe('DELETE /comment/:event_id', () => {
    it('should delete a comment', async () => {
      // First get call:
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc123', text: 'Hi' }] }
        })
      });
      // Then update call:
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } })
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'abc123' });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('message', 'Comment removed successfully.');
      expect(docClient.get).toHaveBeenCalled();
      expect(docClient.update).toHaveBeenCalled();
    });

    it('should return 400 if missing event_id or comment_id', async () => {
      const response = await request(app).delete('/comment/123');
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing event_id or comment_id in request body.');
    });

    it('should return 404 if event not found', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({})
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'some_id' });

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Event not found.');
    });

    it('should return 404 if comment not found', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc', text: 'Random' }] }
        })
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'non_existing' });

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Comment not found.');
    });

    it('should return 500 if update fails', async () => {
      // Get call works
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc123', text: 'Hi' }] }
        })
      });
      // Update call fails
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB update error'))
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'abc123' });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error removing comment.');
    });
  });

  /* --------------------------
   *  POST /user
   * -------------------------- */
  describe('POST /user', () => {
    it('should create a user and return 201', async () => {
      docClient.put.mockReturnValueOnce({ promise: jest.fn().mockResolvedValue({}) });

      const response = await request(app)
        .post('/user')
        .send({
          user_id: 'xyz',
          name: 'Test User',
          location: 'Testville',
          latitude: 12.34,
          longitude: 56.78,
          account_type: 'basic',
          email: 'test@example.com',
          regToken: 'abc123',
          notifications: true
        });

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty('message', 'User created successfully');
      expect(docClient.put).toHaveBeenCalled();
    });

    it('should return 400 if required fields missing', async () => {
      const response = await request(app).post('/user').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if put fails', async () => {
      docClient.put.mockReturnValueOnce({ 
        promise: jest.fn().mockRejectedValue(new Error('Dynamo Error')) 
      });

      const response = await request(app)
        .post('/user')
        .send({
          user_id: 'xyz',
          name: 'Test User',
          location: 'Testville',
          latitude: 12.34,
          longitude: 56.78,
          account_type: 'basic',
          email: 'test@example.com',
          regToken: 'abc123',
          notifications: true
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error creating user');
    });
  });

  /* --------------------------
   *  GET /user/:user_id
   * -------------------------- */
  describe('GET /user/:user_id', () => {
    it('should retrieve a user', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: {
            user_id: 'xyz',
            name: 'Test User'
          }
        })
      });

      const response = await request(app).get('/user/xyz');
      expect(response.status).toBe(200);
      expect(response.body).toEqual({
        user: { user_id: 'xyz', name: 'Test User' }
      });
    });

    it('should return 404 if user not found', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({})
      });

      const response = await request(app).get('/user/xyz');
      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'User not found.');
    });

    it('should return 500 if get fails', async () => {
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error'))
      });

      const response = await request(app).get('/user/xyz');
      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error retrieving user');
    });
  });

  /* --------------------------
   *  POST /user/locations
   * -------------------------- */
  describe('POST /user/locations', () => {
    it('should update user location', async () => {
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { latitude: 12.34, longitude: 56.78 } })
      });

      const response = await request(app)
        .post('/user/locations')
        .send({
          user_id: 'xyz',
          latitude: 12.34,
          longitude: 56.78
        });

      expect(response.status).toBe(200);
      expect(response.body).toHaveProperty('message', 'User location updated successfully');
      expect(docClient.update).toHaveBeenCalled();
    });

    it('should return 400 if missing fields', async () => {
      const response = await request(app).post('/user/locations').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if update fails', async () => {
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error'))
      });

      const response = await request(app)
        .post('/user/locations')
        .send({
          user_id: 'xyz',
          latitude: 12.34,
          longitude: 56.78
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error updating user location');
    });
  });
});
