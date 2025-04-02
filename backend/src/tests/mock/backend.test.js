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
const app = require('../../app');
const AWS = require('aws-sdk');
const axios = require('axios');

describe('Backend Tests (Errors)', () => {
    let docClient;
    let consoleErrorSpy;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        consoleErrorSpy = jest
            .spyOn(console, 'error')
            .mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    /* --------------------------
     *  POST /test_cron
     * -------------------------- */
    describe('POST /test_cron', () => {
        it('should return 500 if an error occurs', async () => {
            // Mock axios.get to reject for testing error handling
            axios.get.mockRejectedValue(new Error('Some fetch error'));

            const response = await request(app).post('/test_cron');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error fetching disaster data'
            );
        });
    });

    /* --------------------------
     *  GET /event
     * -------------------------- */
    describe('GET /event', () => {
        it('should return 500 if an error occurs', async () => {
            // Mock DynamoDB scan to reject
            docClient.scan.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app).get('/event');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error fetching events'
            );
        });
    });

    /* --------------------------
     *  POST /user_marker (was /event/custom)
     * -------------------------- */
    describe('POST /user_marker', () => {
        it('should return 400 if required fields are missing', async () => {
            // In createUserMarker, required fields are type, latitude, longitude, and description
            const response = await request(app).post('/user_marker').send({});
            expect(response.status).toBe(400);
            // The error message from our controller is 'Missing type, latitude, longitude, or description'
            expect(response.body).toHaveProperty(
                'error',
                'Missing type, latitude, longitude, or description'
            );
        });

        it('should return 500 if DynamoDB put fails', async () => {
            // Mock DynamoDB put to reject
            docClient.put.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Put Error')),
            });

            // Send required fields for user marker creation (note: use "type" and "description")
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

    /* --------------------------
     *  GET /event/firms
     * -------------------------- */
    describe('GET /event/firms', () => {
        it('should return 500 if an error occurs fetching data', async () => {
            axios.get.mockRejectedValue(new Error('FIRMS fetch error'));

            const response = await request(app).get('/event/firms');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Failed to fetch FIRMS data'
            );
        });
    });

    /* --------------------------
     *  POST /comment/:id
     * -------------------------- */
    describe('POST /comment/:id', () => {
        it('should return 400 if missing event_id or comment', async () => {
            // The endpoint requires an id param and a comment in the body along with a type query or body parameter.
            const response = await request(app).post('/comment/123').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty(
                'error',
                'Missing required parameters'
            );
        });

        it('should return 500 if DynamoDB update fails', async () => {
            docClient.update.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app)
                .post('/comment/123')
                .send({ comment: 'Hello', type: 'event' });

            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Failed to add comment'
            );
        });
    });

    /* --------------------------
     *  GET /comment/:event_id
     * -------------------------- */
    describe('GET /comment/:event_id', () => {
        it('should return 400 if event_id is missing', async () => {
            // Hitting the route without event_id should return an error (likely a 404)
            const response = await request(app).get('/comment/');
            expect(response.status).toBeGreaterThanOrEqual(400);
        });

        it('should return 404 if no event found', async () => {
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({}),
            });

            const response = await request(app).get('/comment/123');
            expect(response.status).toBe(404);
            expect(response.body).toHaveProperty('error', 'Event not found.');
        });

        it('should return 500 if DynamoDB get fails', async () => {
            docClient.get.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB get error')),
            });

            const response = await request(app).get('/comment/123');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error retrieving comments.'
            );
        });
    });

    /* --------------------------
     *  DELETE /comment/:event_id
     * -------------------------- */
    describe('DELETE /comment/:event_id', () => {
        it('should return 400 if missing event_id or comment_id', async () => {
            const response = await request(app).delete('/comment/123');
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty(
                'error',
                'Missing event_id or comment_id in request body.'
            );
        });

        it('should return 404 if event not found', async () => {
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
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({
                    Item: { comments: [{ comment_id: 'abc123', text: 'Hi' }] },
                }),
            });

            docClient.update.mockReturnValueOnce({
                promise: jest
                    .fn()
                    .mockRejectedValue(new Error('DynamoDB update error')),
            });

            const response = await request(app)
                .delete('/comment/123')
                .send({ comment_id: 'abc123' });

            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty(
                'error',
                'Error removing comment.'
            );
        });
    });

    /* --------------------------
     *  POST /user
     * -------------------------- */
    describe('POST /user', () => {
        it('should return 400 if required fields missing', async () => {
            const response = await request(app).post('/user').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error');
        });

        it('should return 500 if put fails', async () => {
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

    /* --------------------------
     *  GET /user/:user_id
     * -------------------------- */
    describe('GET /user/:user_id', () => {
        it('should return 404 if user not found', async () => {
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({}),
            });

            const response = await request(app).get('/user/xyz');
            expect(response.status).toBe(404);
            expect(response.body).toHaveProperty('error', 'User not found.');
        });

        it('should return 500 if get fails', async () => {
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

    /* --------------------------
     *  POST /user/locations
     * -------------------------- */
    describe('POST /user/locations', () => {
        it('should return 400 if missing fields', async () => {
            const response = await request(app)
                .post('/user/locations')
                .send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error');
        });

        it('should return 500 if update fails', async () => {
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
