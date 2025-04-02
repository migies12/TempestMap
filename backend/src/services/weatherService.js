const axios = require('axios');
const csv = require('csv-parser');
const { v4: uuidv4 } = require('uuid');
const dynamoDB = require('../config/aws');
const notifyUsers = require('../services/notificationService');

const deleteAllEvents = async () => {
    try {
        console.log("Deleting all entries from the 'event' table...");
        const scanParams = {
            TableName: 'event',
            ProjectionExpression: 'event_id',
        };
        const data = await dynamoDB.scan(scanParams).promise();
        if (data.Items && data.Items.length > 0) {
            const deletePromises = data.Items.map((item) => {
                const deleteParams = {
                    TableName: 'event',
                    Key: { event_id: item.event_id },
                };
                return dynamoDB.delete(deleteParams).promise();
            });
            await Promise.all(deletePromises);
            console.log(`Deleted ${data.Items.length} events from the table.`);
        } else {
            console.log('No events found to delete.');
        }
    } catch (error) {
        console.error('Error deleting events:', error);
    }
};

const appendEvents = async (events) => {
    for (const event of events) {
        const params = {
            TableName: 'event',
            Item: {
                event_id: uuidv4(),
                event_type: event.event_type,
                event_name: event.event_name,
                date: event.date,
                lat: event.lat,
                lng: event.lng,
                continent: event.continent,
                country_code: event.country_code,
                created_time: event.created_time,
                source_event_id: event.source_event_id,
                estimated_end_date: event.estimated_end_date,
                comments: [],
            },
        };

        try {
            await dynamoDB.put(params).promise();
            notifyUsers();
            console.log(`Inserted event ${event.event_id}`);
        } catch (error) {
            console.error(`Error inserting event ${event.event_id}:`, error);
        }
    }
};

const fetchDisasterData = async () => {
    try {
        console.log('Cron Job Process Ran');

        const [responseDefault, responseWF] = await Promise.all([
            axios.get(
                'https://api.ambeedata.com/disasters/latest/by-continent',
                {
                    params: {
                        continent: 'NAR',
                        page: 1,
                        limit: 50,
                    },
                    headers: {
                        'x-api-key': `${process.env.AMBEE_API_KEY}`,
                    },
                }
            ),
            axios.get(
                'https://api.ambeedata.com/disasters/latest/by-continent',
                {
                    params: {
                        continent: 'NAR',
                        page: 1,
                        limit: 50,
                        eventType: 'WF',
                    },
                    headers: {
                        'x-api-key': `${process.env.AMBEE_API_KEY}`,
                    },
                }
            ),
        ]);

        await deleteAllEvents();

        const eventsDefault = Array.isArray(responseDefault.data.result)
            ? responseDefault.data.result
            : [];
        const eventsWF = Array.isArray(responseWF.data.result)
            ? responseWF.data.result
            : [];
        const combinedEvents = [...eventsDefault, ...eventsWF];

        console.log(
            'Combined weather/disaster data retrieved:',
            combinedEvents
        );
        if (combinedEvents.length > 0) {
            await appendEvents(combinedEvents);
        } else {
            console.error('No events retrieved from APIs.');
        }
    } catch (error) {
        console.error('Error fetching weather/disaster data:', error);
        throw error;
    }
};

module.exports = { fetchDisasterData };
