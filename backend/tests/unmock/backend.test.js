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
      /*
        Input: None (GET request to "/").
        Expected Status Code: 200
        Expected Output: { message: "Success" }
        Explanation: The root endpoint returns a success message with status 200.
      */
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
      /*
        Input: None (POST request to "/test_cron"; no request body).
        Mock Behavior:
          - axios.get mocked twice to resolve with empty "result" arrays.
          - docClient.scan returns an object with no items and no LastEvaluatedKey.
        Expected Status Code: 200
        Expected Output: { message: "Disaster data fetched successfully!" }
        Explanation: The route calls axios.get to fetch data, uses DynamoDB to scan data.
                     Since both succeed, the response is 200 with a success message.
      */
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // For "default" disasters
      axios.get.mockResolvedValueOnce({ data: { result: [] } }); // For "WF" disasters

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
      /*
        Input: None (GET request to "/event").
        Mock Behavior:
          - docClient.scan resolves with a single event item.
        Expected Status Code: 200
        Expected Output: {
          events: [{ event_id: "123", event_name: "Test Event" }]
        }
        Explanation: If the DynamoDB scan is successful, the server returns 200 with
                     the array of events.
      */
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
   *  POST /event/custom
   * -------------------------- */
  describe('POST /event/custom', () => {
    it('should create a custom event and return 201', async () => {
      /*
        Input: POST request to "/event/custom" with latitude, longitude, markerType.
        Mock Behavior:
          - docClient.put resolves successfully, indicating DynamoDB put was successful.
        Expected Status Code: 201
        Expected Output: {
          message: "Custom event created successfully",
          event: { ...eventFields... }
        }
        Explanation: A valid request with required fields that successfully
                     inserts a new event into DynamoDB returns 201.
      */
      docClient.put.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({}),
      });

      const response = await request(app)
        .post('/event/custom')
        .send({
          latitude: 40.7128,
          longitude: -74.0060,
          markerType: 'TestMarker',
        });

      expect(response.status).toBe(201);
      expect(response.body).toHaveProperty('message', 'Custom event created successfully');
      expect(response.body).toHaveProperty('event');
      expect(docClient.put).toHaveBeenCalled();
    });
  });

  /* --------------------------
   *  GET /event/firms
   * -------------------------- */
  describe('GET /event/firms', () => {
    it('should return 200 and JSON from CSV', async () => {
      /*
        Input: None (GET request to "/event/firms").
        Mock Behavior:
          - axios.get resolves with a mocked stream that emits one row of data.
        Expected Status Code: 200
        Expected Output: An array of rows parsed from CSV, e.g. [{ testField: "value" }]
        Explanation: The route processes a CSV stream from the response, collecting data
                     into a JSON array, then returns it with a 200 status.
      */
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
   *  POST /comment/:event_id
   * -------------------------- */
  describe('POST /comment/:event_id', () => {
    it('should append a comment and return 200', async () => {
      /*
        Input: POST request to "/comment/123" with { comment: "Nice event", user: "John" }.
        Mock Behavior:
          - docClient.update resolves with new Attributes indicating the updated comments array.
        Expected Status Code: 200
        Expected Output: { message: "Comment appended successfully", ... }
        Explanation: Successfully appending a comment to the event’s comments array yields 200.
      */
      docClient.update.mockReturnValueOnce({
        promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } }),
      });

      const response = await request(app)
        .post('/comment/123')
        .send({ comment: 'Nice event', user: 'John' });

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
      /*
        Input: GET request to "/comment/123".
        Mock Behavior:
          - docClient.get resolves with an Item containing comments.
        Expected Status Code: 200
        Expected Output: { comments: [{ text: "Test comment" }] }
        Explanation: Fetches comments from DynamoDB successfully, returning 200.
      */
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
      /*
        Input: DELETE request to "/comment/123" with { comment_id: "abc123" }.
        Mock Behavior:
          - docClient.get resolves with an event that has one comment (abc123).
          - docClient.update resolves, indicating comment removal was successful.
        Expected Status Code: 200
        Expected Output: { message: "Comment removed successfully." }
        Explanation: The route finds and removes the specified comment, returning 200.
      */
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
      /*
        Input: POST request to "/user" with user fields: { user_id, name, location, etc. }
        Mock Behavior:
          - docClient.put resolves successfully, implying user creation.
        Expected Status Code: 201
        Expected Output: { message: "User created successfully" }
        Explanation: Creating a new user in DynamoDB should result in 201 status.
      */
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
          notifications: true,
        });

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
      /*
        Input: GET request to "/user/xyz".
        Mock Behavior:
          - docClient.get resolves with an item containing user details.
        Expected Status Code: 200
        Expected Output: { user: { user_id, name, ... } }
        Explanation: If the user exists in DynamoDB, the route returns their info with 200.
      */
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
      /*
        Input: POST request to "/user/locations" with { user_id, latitude, longitude }.
        Mock Behavior:
          - docClient.update resolves with updated latitude and longitude.
        Expected Status Code: 200
        Expected Output: { message: "User location updated successfully" }
        Explanation: Successful update of a user’s location in DynamoDB yields 200.
      */
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
