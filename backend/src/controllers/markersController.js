const dynamoDB = require('../config/aws');
const { v4: uuidv4 } = require('uuid');

exports.createUserMarker = async (req, res) => {
    const marker_id = req.body.id || req.query.id || uuidv4();
    const type = req.body.type || req.query.type;
    const latitude = req.body.latitude || req.query.latitude;
    const longitude = req.body.longitude || req.query.longitude;
    const description = req.body.description || req.query.description;
    const comments = req.body.comments || req.query.comments || [];

    if (!type || !latitude || !longitude || !description) {
        return res.status(400).json({
            error: 'Missing type, latitude, longitude, or description',
        });
    }

    const newUserMarker = {
        marker_id,
        type,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        description,
        comments: Array.isArray(comments) ? comments : [],
        created_at: new Date().toISOString(),
    };

    const params = {
        TableName: 'user_marker',
        Item: newUserMarker,
    };

    try {
        await dynamoDB.put(params).promise();
        res.status(201).json({
            message: 'User marker created successfully',
            user_marker: newUserMarker,
        });
    } catch (error) {
        console.error('Error creating user marker:', error);
        res.status(500).json({ error: 'Error creating user marker' });
    }
};

exports.getUserMarker = async (req, res) => {
    const params = { TableName: 'user_marker' };
    try {
        const result = await dynamoDB.scan(params).promise();
        const markers = result.Items.map((item) => ({
            id: item.marker_id,
            type: item.type,
            latitude: item.latitude,
            longitude: item.longitude,
            description: item.description,
            comments: item.comments || [],
            createdAt: item.created_at,
        }));
        res.status(200).json({ markers });
    } catch (error) {
        console.error('Error fetching markers:', error);
        res.status(500).json({
            error: 'Failed to fetch markers',
            details: error.message,
        });
    }
};
