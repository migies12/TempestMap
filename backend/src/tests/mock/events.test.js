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

describe('MOCK: Events Route', () => {
    let docClient;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('GET /event', () => {
        it('should return 500 if an error occurs', async () => {
            // Simulate DynamoDB scan failure
            docClient.scan.mockReturnValueOnce({
                promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app).get('/event');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty('error', 'Error fetching events');
        });
    });

    describe('GET /event/firms', () => {
        it('should return 500 if an error occurs fetching data', async () => {
            // Simulate axios.get failure for FIRMS data
            axios.get.mockRejectedValue(new Error('FIRMS fetch error'));

            const response = await request(app).get('/event/firms');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty('error', 'Failed to fetch FIRMS data');
        });
    });
});
