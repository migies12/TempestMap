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

describe('MOCK: Comments Route', () => {
    let docClient;

    beforeAll(() => {
        docClient = new AWS.DynamoDB.DocumentClient();
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'log').mockImplementation(() => {});
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('POST /comment/:id', () => {
        it('should return 400 if missing event_id or comment', async () => {
            // Missing required body parameters
            const response = await request(app).post('/comment/123').send({});
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error', 'Missing required parameters');
        });

        it('should return 500 if DynamoDB update fails', async () => {
            // Simulate failure on DynamoDB update
            docClient.update.mockReturnValueOnce({
                promise: jest.fn().mockRejectedValue(new Error('DynamoDB Error')),
            });

            const response = await request(app)
                .post('/comment/123')
                .send({ comment: 'Hello', type: 'event' });
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty('error', 'Failed to add comment');
        });
    });

    describe('GET /comment/:event_id', () => {
        
        it('should return 400 if event_id is missing', async () => {
            const response = await request(app).get('/comment/');
            expect(response.status).toBeGreaterThanOrEqual(400);
        });

        it('should return 404 if no event found', async () => {
            // Simulate no event found by returning an empty object
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockResolvedValue({}),
            });

            const response = await request(app).get('/comment/123');
            expect(response.status).toBe(404);
            expect(response.body).toHaveProperty('error', 'Event not found.');
        });

        it('should return 400 if missing required parameters', async () => {
            const response = await request(app).post('/comment/123').send({ comment: 'Nice event', user: 'John', type: 'test' });
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty('error', 'Invalid table type');
          });

        
        it('should return 500 if DynamoDB get fails', async () => {
            // Simulate failure on DynamoDB get
            docClient.get.mockReturnValueOnce({
                promise: jest.fn().mockRejectedValue(new Error('DynamoDB get error')),
            });

            const response = await request(app).get('/comment/123');
            expect(response.status).toBe(500);
            expect(response.body).toHaveProperty('error', 'Error retrieving comments.');
        });
    });

    describe('DELETE /comment/:event_id', () => {
        it('should return 400 if missing event_id or comment_id', async () => {
            const response = await request(app).delete('/comment/123');
            expect(response.status).toBe(400);
            expect(response.body).toHaveProperty(
                'error',
                'Missing event_id or comment_id in request body.',
            );
        });

        it('should return 404 if event not found', async () => {
            // Simulate no event found
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
            // Simulate event found but comment not found
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
            // Simulate event found with a comment, then a failure on update
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
});
