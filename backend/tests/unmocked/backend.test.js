const request = require('supertest');
const app = require('backend/tempest.js'); // Adjust the path as necessary

describe('GET / (No Mocks) - Tested Interface: Root Endpoint', () => {
  /**
   * Test: GET /
   * Inputs: No parameters (simple GET request)
   * Expected Status Code: 200
   * Expected Output: { message: "Success" }
   * Expected Behavior: The server responds with a success message without involving external components.
   */
  it('should return status 200 and a success message', async () => {
    const response = await request(app).get('/');
    expect(response.status).toBe(200);
    expect(response.body).toEqual({ message: "Success" });
  });
});