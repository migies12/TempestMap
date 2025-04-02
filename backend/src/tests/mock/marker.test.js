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

describe('MOCK: Marker Route', () => {
    let docClient;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /user_marker', () => {
        it('should return 400 if required fields are missing', async () => {
            const response = await request(app).post('/user_marker').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty(
                'error',
                'Missing type, latitude, longitude, or description'
            );
        });

        it('should return 500 if DynamoDB put fails', async () => {
            // Simulate failure on DynamoDB put
            docClient.put.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Put Error')),
            });

            const response = await request(app).post('/user_marker').send({
                type: 'TestMarker',
                latitude: 40.7128,
                longitude: -74.006,
                description: 'Test Description',
            });

            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error creating user marker'
            );
        });
    });

    it('should return 500 if an error occurs during scan', async () => {
        const errorMessage = 'Test error message';

        // Mock the dynamoDB.scan method to reject with an error
        jest.spyOn(docClient, 'scan').mockReturnValue({
            promise: () => Promise.reject(new Error(errorMessage)),
        });

        // Make the GET request through the route
        const response = await request(app).get('/user_marker');

        // Assert that the error is handled properly
        expect(response.status).toBe(500);
        expect(response.body).toHaveProperty('error', 'Failed to fetch markers');
        expect(response.body).toHaveProperty('details', errorMessage);
    });
});
