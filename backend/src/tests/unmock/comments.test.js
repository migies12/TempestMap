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
  
  describe('UNMOCKED: Comments Route', () => {
    let docClient;
    
    beforeAll(() => {
      docClient = new AWS.DynamoDB.DocumentClient();
      jest.spyOn(console, 'error').mockImplementation(() => {});
      jest.spyOn(console, 'log').mockImplementation(() => {});
    });
  
    beforeEach(() => {
      jest.clearAllMocks();
    });
  
    /* --------------------------
     *  POST /comment/:id
     * -------------------------- */
    describe('POST /comment/:id', () => {
  
      it('append a comment (event) and return 200', async () => {
        docClient.update.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } }),
        });
  
        const response = await request(app)
          .post('/comment/123')
          .send({ comment: 'Nice event', user: 'John', type: 'event' });
  
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty(
          'message',
          'Comment appended successfully'
        );
        expect(docClient.update).toHaveBeenCalled();
      });

      it('ppend a comment (user_marker) and return 200', async () => {
        docClient.update.mockReturnValueOnce({
          promise: jest.fn().mockResolvedValue({ Attributes: { comments: [] } }),
        });
  
        const response = await request(app)
          .post('/comment/123')
          .send({ comment: 'Nice event', user: 'John', type: 'user_marker' });
  
        expect(response.status).toBe(200);
        expect(response.body).toHaveProperty(
          'message',
          'Comment appended successfully'
        );
        expect(docClient.update).toHaveBeenCalled();
      });
    });
  
    /* --------------------------
     *  GET /comment/:event_id
     * -------------------------- */
    describe('GET /comment/:event_id', () => {
      it('should retrieve comments for the event', async () => {
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
        expect(response.body).toHaveProperty(
          'message',
          'Comment removed successfully.'
        );
        expect(docClient.get).toHaveBeenCalled();
        expect(docClient.update).toHaveBeenCalled();
      });
    });
  });
  