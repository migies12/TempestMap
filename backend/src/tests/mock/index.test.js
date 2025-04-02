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
const axios = require('axios');

describe('MOCK: Index Route', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    // describe('POST /test_cron', () => {
    //     it('should return 500 if an error occurs', async () => {
    //         // Force axios.get to fail to simulate error handling
    //         axios.get.mockRejectedValue(new Error('Some fetch error'));

    //         const response = await request(app).post('/test_cron');
    //         expect(response.status).toBe(500);
    //         expect(response.body).toHaveProperty('error', 'Error fetching disaster data');
    //     });
    // });

    // Optionally, you can add a test for GET / (home) if needed.
    describe('GET /', () => {
        it('should return 200 and a home message', async () => {
            const response = await request(app).get('/');
            expect(response.status).toBe(200);
            // Add assertions for expected response body if applicable
        });
    });
});
