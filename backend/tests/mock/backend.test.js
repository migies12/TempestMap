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

describe('Backend Tests (Errors)', () => {
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
   *  POST /test_cron
   * -------------------------- */
  describe('POST /test_cron', () => {
    it('should return 500 if an error occurs', async () => {
      /*
        Input: None (POST request to /test_cron without a request body).
        Mock Behavior:
          - axios.get is mocked to reject with a new Error('Some fetch error').
        Expected Status Code: 500
        Expected Output: { error: "Error fetching disaster data" }
        Explanation: The route attempts to fetch data using axios. If axios fails,
                     the server responds with a 500 status and an error message.
      */
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
    it('should return 500 if an error occurs', async () => {
      /*
        Input: None (GET request to /event).
        Mock Behavior:
          - docClient.scan is mocked to reject with a new Error('DynamoDB Error').
        Expected Status Code: 500
        Expected Output: { error: "Error fetching events" }
        Explanation: The route attempts to scan the "events" table in DynamoDB.
                     If that operation fails, a 500 error is returned.
      */
      docClient.scan.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
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
    it('should return 400 if required fields are missing', async () => {
      /*
        Input: An empty body (POST request to /event/custom with {}).
        Expected Status Code: 400
        Expected Output: Body contains 'error' indicating missing fields.
        Explanation: The request body must contain latitude, longitude, and markerType.
                     If these fields are missing, the server should respond with 400.
      */
      const response = await request(app).post('/event/custom').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if DynamoDB put fails', async () => {
      /*
        Input: { latitude, longitude, markerType } in request body.
        Mock Behavior:
          - docClient.put is mocked to reject with new Error('DynamoDB Put Error').
        Expected Status Code: 500
        Expected Output: { error: "Error creating custom event" }
        Explanation: When DynamoDB put fails, the route catches the error and returns 500.
      */
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Put Error')),
      });

      const response = await request(app)
        .post('/event/custom')
        .send({
          latitude: 40.7128,
          longitude: -74.0060,
          markerType: 'TestMarker',
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error creating custom event');
    });
  });

  /* --------------------------
   *  GET /event/firms
   * -------------------------- */
  describe('GET /event/firms', () => {
    it('should return 500 if an error occurs fetching data', async () => {
      /*
        Input: None (GET request to /event/firms).
        Mock Behavior:
          - axios.get is mocked to reject with new Error('FIRMS fetch error').
        Expected Status Code: 500
        Expected Output: { error: "Failed to fetch FIRMS data" }
        Explanation: If the external call to FIRMS API fails, a 500 error is returned.
      */
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
    it('should return 400 if missing event_id or comment', async () => {
      /*
        Input: Request URL includes an event_id (e.g. "/comment/123"), but body is empty.
        Expected Status Code: 400
        Expected Output: { error: "Missing event_id or comment in request body" }
        Explanation: The server requires an event_id in the URL, but also a "comment" field
                     in the body. Missing either yields a 400 response.
      */
      const response = await request(app).post('/comment/123').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing event_id or comment in request body');
    });

    it('should return 500 if DynamoDB update fails', async () => {
      /*
        Input: URL includes event_id=123, body includes { comment: "Hello" }.
        Mock Behavior:
          - docClient.update is mocked to reject with new Error('DynamoDB Error').
        Expected Status Code: 500
        Expected Output: { error: "Error appending comment" }
        Explanation: On a DynamoDB update failure, the route responds with 500.
      */
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
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
    it('should return 400 if event_id is missing', async () => {
      /*
        Input: GET request made to "/comment/" (no event_id in the path).
        Expected Status Code: 4xx (likely 404, but can be 400 depending on how routes are configured).
        Expected Output: An error or not found response.
        Explanation: The route definition likely expects an event_id parameter.
                     Hitting "/comment/" directly should return an error.
      */
      const response = await request(app).get('/comment/');
      expect(response.status).toBeGreaterThanOrEqual(400);
    });

    it('should return 404 if no event found', async () => {
      /*
        Input: GET request to "/comment/123".
        Mock Behavior:
          - docClient.get is mocked to resolve to an empty object.
        Expected Status Code: 404
        Expected Output: { error: "Event not found." }
        Explanation: If DynamoDB returns no Item, the event doesn't exist.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({}),
      });

      const response = await request(app).get('/comment/123');
      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Event not found.');
    });

    it('should return 500 if DynamoDB get fails', async () => {
      /*
        Input: GET request to "/comment/123".
        Mock Behavior:
          - docClient.get is mocked to reject with new Error('DynamoDB get error').
        Expected Status Code: 500
        Expected Output: { error: "Error retrieving comments." }
        Explanation: If the call to DynamoDB fails, a 500 error is returned.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB get error')),
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
    it('should return 400 if missing event_id or comment_id', async () => {
      /*
        Input: DELETE request to "/comment/123" without a comment_id in the body.
        Expected Status Code: 400
        Expected Output: { error: "Missing event_id or comment_id in request body." }
        Explanation: The route requires both the event_id in the URL and the comment_id in the body.
      */
      const response = await request(app).delete('/comment/123');
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error', 'Missing event_id or comment_id in request body.');
    });

    it('should return 404 if event not found', async () => {
      /*
        Input: DELETE request to "/comment/123", body includes { comment_id: "some_id" }.
        Mock Behavior:
          - docClient.get is mocked to resolve to an empty object, implying no event found.
        Expected Status Code: 404
        Expected Output: { error: "Event not found." }
        Explanation: If the event does not exist, the server returns 404.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({}),
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'some_id' });

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Event not found.');
    });

    it('should return 404 if comment not found', async () => {
      /*
        Input: DELETE request to "/comment/123", body includes { comment_id: "non_existing" }.
        Mock Behavior:
          - docClient.get is mocked to resolve to an item with a different comment_id.
        Expected Status Code: 404
        Expected Output: { error: "Comment not found." }
        Explanation: If the provided comment_id doesn’t match any comment in the event, 404 is returned.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc', text: 'Random' }] },
        }),
      });

      const response = await request(app)
        .delete('/comment/123')
        .send({ comment_id: 'non_existing' });

      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'Comment not found.');
    });

    it('should return 500 if update fails', async () => {
      /*
        Input: DELETE request to "/comment/123", body includes { comment_id: "abc123" }.
        Mock Behavior:
          - docClient.get is mocked to resolve to an item containing the comment.
          - docClient.update is mocked to reject with new Error('DynamoDB update error').
        Expected Status Code: 500
        Expected Output: { error: "Error removing comment." }
        Explanation: Even if the comment exists, if the update operation fails, 500 is returned.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({
          Item: { comments: [{ comment_id: 'abc123', text: 'Hi' }] },
        }),
      });

      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB update error')),
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
    it('should return 400 if required fields missing', async () => {
      /*
        Input: POST request to "/user" with an empty body.
        Expected Status Code: 400
        Expected Output: Body contains 'error' about missing fields.
        Explanation: The route requires user_id, name, location, and more.
                     Missing them results in a 400 response.
      */
      const response = await request(app).post('/user').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if put fails', async () => {
      /*
        Input: Valid fields for a user in the request body.
        Mock Behavior:
          - docClient.put is mocked to reject with new Error('Dynamo Error').
        Expected Status Code: 500
        Expected Output: { error: "Error creating user" }
        Explanation: If storing the new user fails in DynamoDB, the server returns a 500.
      */
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('Dynamo Error')),
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
          notifications: true,
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error creating user');
    });
  });

  /* --------------------------
   *  GET /user/:user_id
   * -------------------------- */
  describe('GET /user/:user_id', () => {
    it('should return 404 if user not found', async () => {
      /*
        Input: GET request to "/user/xyz".
        Mock Behavior:
          - docClient.get resolves to an empty object, indicating user not found.
        Expected Status Code: 404
        Expected Output: { error: "User not found." }
        Explanation: If the user doesn’t exist in DynamoDB, we return 404.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({}),
      });

      const response = await request(app).get('/user/xyz');
      expect(response.status).toBe(404);
      expect(response.body).toHaveProperty('error', 'User not found.');
    });

    it('should return 500 if get fails', async () => {
      /*
        Input: GET request to "/user/xyz".
        Mock Behavior:
          - docClient.get is mocked to reject with new Error('DynamoDB Error').
        Expected Status Code: 500
        Expected Output: { error: "Error retrieving user" }
        Explanation: If the database call throws an error, return 500.
      */
      docClient.get.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
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
    it('should return 400 if missing fields', async () => {
      /*
        Input: POST request to "/user/locations" with empty or incomplete body.
        Expected Status Code: 400
        Expected Output: Body contains 'error' about missing fields.
        Explanation: We expect user_id, latitude, and longitude to be present.
                     If they aren’t, a 400 error is returned.
      */
      const response = await request(app).post('/user/locations').send({});
      expect(response.status).toBe(400);
      expect(response.body).toHaveProperty('error');
    });

    it('should return 500 if update fails', async () => {
      /*
        Input: POST request with the required fields: { user_id, latitude, longitude }.
        Mock Behavior:
          - docClient.update is mocked to reject with new Error('DynamoDB Error').
        Expected Status Code: 500
        Expected Output: { error: "Error updating user location" }
        Explanation: If DynamoDB update fails, the route responds with 500.
      */
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
      });

      const response = await request(app)
        .post('/user/locations')
        .send({
          user_id: 'xyz',
          latitude: 12.34,
          longitude: 56.78,
        });

      expect(response.status).toBe(500);
      expect(response.body).toHaveProperty('error', 'Error updating user location');
    });
  });
});
