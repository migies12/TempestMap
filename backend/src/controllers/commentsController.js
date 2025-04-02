const dynamoDB = require('../config/aws');
const { v4: uuidv4 } = require('uuid');

exports.addComment = async (req, res) => {
    const type = req.body.type || req.query.type;
    const id = req.params.id;
    const comment = req.body.comment || req.query.comment;
    const user = req.body.user || req.query.user;

    if (!id || !comment || !type) {
        return res.status(400).json({
            error: 'Missing required parameters',
            received: { id, comment, type },
        });
    }

    let keyName;
    if (type === 'event') {
        keyName = 'event_id';
    } else if (type === 'user_marker') {
        keyName = 'marker_id';
    } else {
        return res.status(400).json({ error: 'Invalid table type' });
    }

    const newComment = {
        comment_id: uuidv4(),
        text: comment,
        user: user || 'anonymous',
        created_at: new Date().toISOString(),
    };

    const params = {
        TableName: type,
        Key: { [keyName]: id },
        UpdateExpression:
            'SET #comments = list_append(if_not_exists(#comments, :emptyList), :newComment)',
        ExpressionAttributeNames: { '#comments': 'comments' },
        ExpressionAttributeValues: {
            ':emptyList': [],
            ':newComment': [newComment],
        },
        ReturnValues: 'UPDATED_NEW',
    };

    try {
        const result = await dynamoDB.update(params).promise();
        res.status(200).json({
            message: 'Comment appended successfully',
            comment: newComment,
            updated: result.Attributes,
        });
    } catch (error) {
        console.error('Error adding comment:', error);
        res.status(500).json({
            error: 'Failed to add comment',
            details: error.message,
        });
    }
};

exports.getComments = async (req, res) => {
    const { event_id } = req.params;

    if (!event_id) {
        return res
            .status(400)
            .json({ error: 'Missing event_id in the URL parameter.' });
    }

    try {
        const params = {
            TableName: 'event',
            Key: { event_id },
            ProjectionExpression: 'comments',
        };
        const result = await dynamoDB.get(params).promise();
        if (!result.Item) {
            return res.status(404).json({ error: 'Event not found.' });
        }
        res.status(200).json({
            event_id,
            comments: result.Item.comments || [],
        });
    } catch (error) {
        console.error('Error retrieving comments:', error);
        res.status(500).json({ error: 'Error retrieving comments.' });
    }
};

exports.deleteComment = async (req, res) => {
    const { event_id } = req.params;
    const comment_id = req.body.comment_id || req.query.comment_id;
    if (!event_id || !comment_id) {
        return res
            .status(400)
            .json({ error: 'Missing event_id or comment_id in request body.' });
    }

    try {
        const getParams = {
            TableName: 'event',
            Key: { event_id },
            ProjectionExpression: 'comments',
        };
        const data = await dynamoDB.get(getParams).promise();
        if (!data.Item) {
            return res.status(404).json({ error: 'Event not found.' });
        }
        const comments = data.Item.comments || [];
        const commentIndex = comments.findIndex(
            (c) => c.comment_id === comment_id
        );
        if (commentIndex === -1) {
            return res.status(404).json({ error: 'Comment not found.' });
        }
        const updateParams = {
            TableName: 'event',
            Key: { event_id },
            UpdateExpression: `REMOVE comments[${commentIndex}]`,
            ReturnValues: 'UPDATED_NEW',
        };
        const updateResult = await dynamoDB.update(updateParams).promise();
        res.status(200).json({
            message: 'Comment removed successfully.',
            updatedAttributes: updateResult.Attributes,
        });
    } catch (error) {
        console.error('Error removing comment:', error);
        res.status(500).json({ error: 'Error removing comment.' });
    }
};
