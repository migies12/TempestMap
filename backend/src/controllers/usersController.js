const dynamoDB = require('../config/aws');
const { v4: uuidv4 } = require('uuid');
const notifyUsers = require('../services/notificationService');

exports.createUser = async (req, res) => {
    let user_id = req.body.user_id || req.query.user_id;
    const name = req.body.name || req.query.name;
    const location = req.body.location || req.query.location;
    const latitude = req.body.latitude || req.query.latitude;
    const longitude = req.body.longitude || req.query.longitude;
    const account_type = req.body.account_type || req.query.account_type;
    const email = req.body.email || req.query.email;
    const regToken = req.body.regToken || req.query.regToken;
    const notifications = req.body.notifications;

    if (
        !name ||
        !location ||
        !account_type ||
        !email ||
        !regToken ||
        notifications == null
    ) {
        return res
            .status(400)
            .json({ error: 'Missing required fields in request body' });
    }

    if (!user_id) {
        user_id = uuidv4();
    }

    const newUser = {
        user_id,
        name,
        location,
        latitude,
        longitude,
        account_type,
        email,
        regToken,
        notifications,
        created_at: new Date().toISOString(),
    };

    const params = {
        TableName: 'user',
        Item: newUser,
    };

    try {
        await dynamoDB.put(params).promise();
        // Optionally send notifications after user creation
        notifyUsers();
        res.status(201).json({
            message: 'User created successfully',
            user: newUser,
        });
    } catch (error) {
        console.error('Error creating user:', error);
        res.status(500).json({ error: 'Error creating user' });
    }
};

exports.getUser = async (req, res) => {
    const { user_id } = req.params;
    if (!user_id) {
        return res
            .status(400)
            .json({ error: 'Missing user_id in URL parameter.' });
    }

    const params = {
        TableName: 'user',
        Key: { user_id },
    };

    try {
        const result = await dynamoDB.get(params).promise();
        if (!result.Item) {
            return res.status(404).json({ error: 'User not found.' });
        }
        res.status(200).json({ user: result.Item });
    } catch (error) {
        console.error('Error retrieving user:', error);
        res.status(500).json({ error: 'Error retrieving user' });
    }
};

exports.updateUserLocation = async (req, res) => {
    const { user_id, latitude, longitude } = req.body;
    if (!user_id || !latitude || !longitude) {
        return res.status(400).json({
            error: 'Missing user_id, latitude, or longitude in request body.',
        });
    }

    const params = {
        TableName: 'user',
        Key: { user_id },
        UpdateExpression: 'SET latitude = :lat, longitude = :lng',
        ExpressionAttributeValues: {
            ':lat': parseFloat(latitude),
            ':lng': parseFloat(longitude),
        },
        ReturnValues: 'UPDATED_NEW',
    };

    try {
        const result = await dynamoDB.update(params).promise();
        res.status(200).json({
            message: 'User location updated successfully',
            updatedAttributes: result.Attributes,
        });
    } catch (error) {
        console.error('Error updating user location:', error);
        res.status(500).json({ error: 'Error updating user location' });
    }
};
