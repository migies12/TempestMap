const express = require('express');
const AWS = require('aws-sdk');
const cron = require('node-cron');
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

// SETUP
const app = express();
app.use(express.json()); // Middleware to parse JSON request bodies

// AWS
AWS.config.update({ region: 'us-west-1' });
const dynamoDB = new AWS.DynamoDB.DocumentClient();

/* --- API Routes --- */

app.post('/test_append', (req, res) => {

  fetchDisasterData()
  res.status(200).json({ message: "Success!" });
});

app.post('/comment', async (req, res) => {
  // Destructure the event_id and comment text from the request body
  const { event_id, comment, user} = req.body;
  
  if (!event_id || !comment) {
    return res.status(400).json({ error: 'Missing event_id or comment in request body' });
  }
  
  // Create a comment object that includes a unique comment_id and timestamp
  const newComment = {
    comment_id: uuidv4(),
    text: comment,
    user: user,
    created_at: new Date().toISOString(),
  };

  const params = {
    TableName: 'event',
    Key: { event_id },
    UpdateExpression: 'SET comments = list_append(if_not_exists(comments, :emptyList), :newComment)',
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
      updatedAttributes: result.Attributes,
    });
  } catch (error) {
    console.error('Error appending comment:', error);
    res.status(500).json({ error: 'Error appending comment' });
  }
});

/* --- Helper Functions --- */

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
      console.log("No events found to delete.");
    }
  } catch (error) {
    console.error("Error deleting events:", error);
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
      console.log(`Inserted event ${event.event_id}`);
    } catch (error) {
      console.error(`Error inserting event ${event.event_id}:`, error);
    }
  }
};

const fetchDisasterData = async () => {
  try {
    console.log("Cron Job Process Ran");

    await deleteAllEvents();

    const response = await axios.get('https://api.ambeedata.com/disasters/latest/by-continent', {
      params: {
        continent: 'NAR',
        page: 1,
        limit: 50,
      },
      headers: {
        'x-api-key': '2589d0a50837e5fcf4d5b249f289ca84dd20bf924b5987c2bad7141eca095041',
      },
    });
    console.log('Weather/Disaster data retrieved:', response.data);

    const events = response.data.result;
    if (Array.isArray(events)) {
      await appendEvents(events);
    } else {
      console.error("Expected 'result' to be an array of events.");
    }
  } catch (error) {
    console.error('Error fetching weather/disaster data:', error);
  }
};

// Schedule the job to run at the start of every hour
cron.schedule('0 * * * *', fetchDisasterData);

app.listen(3000, () => {
  console.log("Tempest Backend: Server running on port 3000");
});
