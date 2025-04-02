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

describe('MOCK: Users Route', () => {
    let docClient;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /user', () => {
        it('should return 400 if required fields missing', async () => {
            const response = await request(app).post('/user').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error');
        });

        it('should return 500 if put fails', async () => {
            // Simulate DynamoDB put failure
            docClient.put.mockReturnValueOnce({
                promise: jest.fn().mockRejectedValue(new Error('Dynamo Error')),
            });

            const response = await request(app).post('/user').send({
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
            expect(response.body).toHaveProperty(
                'error',
                'Error creating user'
            );
        });
    });

    describe('GET /user/:user_id', () => {
        it('should return 404 if user not found', async () => {
            // Simulate no user found
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({}),
            });

            const response = await request(app).get('/user/xyz');
            expect(response.status).toBe(404);
            expect(response.body).toHaveProperty('error', 'User not found.');
        });

        it('should return 500 if get fails', async () => {
            // Simulate DynamoDB get failure
            docClient.get.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app).get('/user/xyz');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error retrieving user'
            );
        });
    });

    describe('POST /user/locations', () => {
        it('should return 400 if missing fields', async () => {
            const response = await request(app)
                .post('/user/locations')
                .send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error');
        });

        it('should return 500 if update fails', async () => {
            // Simulate failure on DynamoDB update
            docClient.update.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app).post('/user/locations').send({
                user_id: 'xyz',
                latitude: 12.34,
                longitude: 56.78,
            });
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error updating user location'
            );
        });
    });
});
