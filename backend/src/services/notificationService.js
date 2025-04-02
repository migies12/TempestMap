const dynamoDB = require('../config/aws');
const { getMessaging } = require('../config/firebase');
const dangerLevelCalc = require('../utils/dangerLevelCalc');

const notifyUsers = async () => {
    console.log("in notifyUsers");

    let events = [];
    try {
        const params = { TableName: 'event' };
        let scanResults;
        do {
            scanResults = await dynamoDB.scan(params).promise();
            events = events.concat(scanResults.Items);
            params.ExclusiveStartKey = scanResults.LastEvaluatedKey;
        } while (scanResults.LastEvaluatedKey);
    } catch (error) {
        console.error('Error fetching events:', error);
        return;
    }

    let users = [];
    try {
        const params = { TableName: 'user' };
        let scanResults;
        do {
            scanResults = await dynamoDB.scan(params).promise();
            users = users.concat(scanResults.Items);
            params.ExclusiveStartKey = scanResults.LastEvaluatedKey;
        } while (scanResults.LastEvaluatedKey);
    } catch (error) {
        console.error('Error fetching users:', error);
        return;
    }

    for (const user of users) {
        if (!user.notifications) continue;
        for (const event of events) {
            const dangerLevel = dangerLevelCalc(
                event.lat,
                event.lng,
                user.latitude,
                user.longitude,
                event.event_type
            );
            if (dangerLevel > 0) {
                console.log(
                    `lat1: ${event.lat}, lon1: ${event.lng}, lat2: ${user.latitude}, lon2: ${user.longitude}, disaster: ${event.event_type}`
                );
                console.log('Danger Level: ', dangerLevel);
                console.log('RegToken: ', user.regToken);
                const message = {
                    notification: {
                        title: `WARNING: ${event.event_type} detected near your location!`,
                        body: `${event.event_type} detected. Lat: ${event.lat.toFixed(
                            2
                        )}, Lng: ${event.lng.toFixed(2)}`,
                    },
                    token: user.regToken,
                    android: {
                        notification: {
                            tag: `event_${event.event_id}`,
                        },
                    },
                };

                getMessaging()
                    .send(message)
                    .then((response) => {
                        console.log('Successfully sent message:', response);
                    })
                    .catch((error) => {
                        console.error('Error sending message:', error);
                    });
            }
        }
    }
};

module.exports = notifyUsers;
