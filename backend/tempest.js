const express = require('express');
const AWS = require('aws-sdk');
const cron = require('node-cron');
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

// SETUP
const app = express();
app.use(express.json()); 

// AWS
require('aws-sdk/lib/maintenance_mode_message').suppress = true;
AWS.config.update({ region: 'us-west-1' });
const dynamoDB = new AWS.DynamoDB.DocumentClient();


/* --- API Routes --- */

// TESTING ROUTE FOR CRON JOB
app.post('/test_cron', async (req, res) => {
  try {
    await fetchDisasterData();
    res.status(200).json({ message: "Disaster data fetched successfully!" });
  } catch (error) {
    console.error('Error in fetchDisasterData:', error);
    res.status(500).json({ error: "Error fetching disaster data" });
  }
});

app.get('/event', async (req, res) => {
  try {
    const params = {
      TableName: 'event'
    };

    let items = [];
    let scanResults;
    do {
      scanResults = await dynamoDB.scan(params).promise();
      items = items.concat(scanResults.Items);
      params.ExclusiveStartKey = scanResults.LastEvaluatedKey;
    } while (scanResults.LastEvaluatedKey);

    res.status(200).json({ events: items });
  } catch (error) {
    console.error('Error fetching events:', error);
    res.status(500).json({ error: 'Error fetching events' });
  }
});


app.post('/comment/:event_id', async (req, res) => {

  const { event_id } = req.params;
  const comment = req.body.comment || req.query.comment;
  const user = req.body.user || req.query.user;
  
  if (!event_id || !comment) {
    console.log(event_id)
    console.log(comment)
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

app.get('/comment/:event_id', async (req, res) => {
  const { event_id } = req.params;

  if (!event_id) {
    return res.status(400).json({ error: 'Missing event_id in the URL parameter.' });
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
      comments: result.Item.comments || []
    });
  } catch (error) {
    console.error('Error retrieving comments:', error);
    res.status(500).json({ error: 'Error retrieving comments.' });
  }
});

app.delete('/comment/:event_id', async (req, res) => {
  
  const { event_id } = req.params;
  const comment_id = req.body.comment_id || req.query.comment_id;
  
  if (!event_id || !comment_id) {
    return res.status(400).json({ error: 'Missing event_id or comment_id in request body.' });
  }
  
  try {
    // Retrieve the event item, focusing on the comments attribute
    const getParams = {
      TableName: 'event',
      Key: { event_id },
      ProjectionExpression: 'comments'
    };
    
    const data = await dynamoDB.get(getParams).promise();
    if (!data.Item) {
      return res.status(404).json({ error: 'Event not found.' });
    }
    
    const comments = data.Item.comments || [];
    // Find the index of the comment with the provided comment_id
    const commentIndex = comments.findIndex(c => c.comment_id === comment_id);
    if (commentIndex === -1) {
      return res.status(404).json({ error: 'Comment not found.' });
    }
    
    // Use an update expression to remove the comment at the identified index
    const updateParams = {
      TableName: 'event',
      Key: { event_id },
      UpdateExpression: `REMOVE comments[${commentIndex}]`,
      ReturnValues: 'UPDATED_NEW'
    };
    
    const updateResult = await dynamoDB.update(updateParams).promise();
    res.status(200).json({
      message: 'Comment removed successfully.',
      updatedAttributes: updateResult.Attributes
    });
  } catch (error) {
    console.error('Error removing comment:', error);
    res.status(500).json({ error: 'Error removing comment.' });
  }
});

app.post('/user', async (req, res) => {

  const name = req.body.name || req.query.name;
  const location = req.body.location || req.query.location;
  const account_type = req.body.account_type || req.query.account_type;
  
  if (!name || !location || !account_type) {
    return res.status(400).json({ error: 'Missing name, location, or account_type in request body' });
  }
  
  const newUser = {
    user_id: uuidv4(),
    name,
    location,
    account_type,
    created_at: new Date().toISOString()
  };

  const params = {
    TableName: 'user', 
    Item: newUser
  };

  try {
    await dynamoDB.put(params).promise();
    res.status(201).json({ message: 'User created successfully', user: newUser });
  } catch (error) {
    console.error('Error creating user:', error);
    res.status(500).json({ error: 'Error creating user' });
  }
});

app.get('/user/:user_id', async (req, res) => {
  const { user_id } = req.params;
  
  if (!user_id) {
    return res.status(400).json({ error: 'Missing user_id in URL parameter.' });
  }

  const params = {
    TableName: 'user',
    Key: { user_id }
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

    const [responseDefault, responseWF] = await Promise.all([
      axios.get('https://api.ambeedata.com/disasters/latest/by-continent', {
        params: {
          continent: 'NAR',
          page: 1,
          limit: 50,
        },
        headers: {
          'x-api-key': '2589d0a50837e5fcf4d5b249f289ca84dd20bf924b5987c2bad7141eca095041',
        },
      }),
      axios.get('https://api.ambeedata.com/disasters/latest/by-continent', {
        params: {
          continent: 'NAR',
          page: 1,
          limit: 50,
          eventType: 'WF',
        },
        headers: {
          'x-api-key': '2589d0a50837e5fcf4d5b249f289ca84dd20bf924b5987c2bad7141eca095041',
        },
      }),
    ]);

    await deleteAllEvents();

    const eventsDefault = Array.isArray(responseDefault.data.result)
      ? responseDefault.data.result
      : [];
    const eventsWF = Array.isArray(responseWF.data.result)
      ? responseWF.data.result
      : [];

    const combinedEvents = [...eventsDefault, ...eventsWF];

    console.log('Combined weather/disaster data retrieved:', combinedEvents);

    if (combinedEvents.length > 0) {
      await appendEvents(combinedEvents);
    } else {
      console.error("No events retrieved from APIs.");
    }
  } catch (error) {
    console.error('Error fetching weather/disaster data:', error);
  }
};

// Schedule the job to run at the start of every hour
cron.schedule('0 * * * *', fetchDisasterData);

app.listen(80, () => {
  console.log("Tempest Backend: Server running on port 80");
});
