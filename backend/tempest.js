const express = require('express');
const AWS = require('aws-sdk');
const cron = require('node-cron');
const axios = require('axios');
const csv = require('csv-parser');
const { v4: uuidv4 } = require('uuid');
const { initializeApp, applicationDefault } = require('firebase-admin/app')
const { getMessaging } = require('firebase-admin/messaging');

// SETUP
const app = express();
const firebase = initializeApp({
  credential: applicationDefault()
});
app.use(express.json()); 

// AWS
require('aws-sdk/lib/maintenance_mode_message').suppress = true;
AWS.config.update({ region: 'us-west-1' });
const dynamoDB = new AWS.DynamoDB.DocumentClient();


/* --- API Routes --- */
app.get('/', async (req, res) => {
  try {
    res.status(200).json({message: "Success"});
  } catch (error) {
    console.error('Error', error);
    res.status(500).json({ error: 'Error' });
  }
});


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

//Adds new custom marker information to the DB
app.post('/event/custom', async (req, res) => {
  const { latitude, longitude, markerType } = req.body;

  // Validate required fields
  if (!latitude || !longitude || !markerType) {
    return res.status(400).json({ error: 'Missing latitude, longitude, or markerType in request body.' });
  }

  // Create a new event object
  const newEvent = {
    event_id: uuidv4(), // Generate a unique event ID
    event_type: markerType, 
    event_name: `Custom ${markerType} Event`, 
    date: new Date().toISOString(), 
    lat: parseFloat(latitude),
    lng: parseFloat(longitude), 
    continent: 'Custom', 
    country_code: 'N/A',
    created_time: new Date().toISOString(),
    source_event_id: 'custom',
    estimated_end_date: null, 
    comments: [], 
  };

  // Define DynamoDB parameters
  const params = {
    TableName: 'event',
    Item: newEvent,
  };

  try {
    // Insert the new event into the DynamoDB table
    await dynamoDB.put(params).promise();
    res.status(201).json({ message: 'Custom event created successfully', event: newEvent });
  } catch (error) {
    console.error('Error creating custom event:', error);
    res.status(500).json({ error: 'Error creating custom event' });
  }
});


// Endpoint to fetch FIRMS data
app.get('/event/firms', async (req, res) => {
  console.log('Received request at /event/firms');
  try {
      const currentDate = new Date().toISOString().split('T')[0]; // Get current date
      const url = `https://firms.modaps.eosdis.nasa.gov/api/area/csv/840168c717a27d2e1ed7faf3744dc8cc/VIIRS_NOAA21_NRT/world/10/${currentDate}`;
      console.log('Fetching FIRMS data from:', url);

      const response = await axios.get(url, { responseType: 'stream' });
      const results = [];

      // Parse the CSV response
      response.data
          .pipe(csv())
          .on('data', (data) => results.push(data))
          .on('end', () => {
              console.log('FIRMS data fetched and parsed successfully:', results);
              res.json(results); // Send JSON response
          })
          .on('error', (error) => {
              console.error('Error parsing CSV:', error);
              res.status(500).json({ error: 'Failed to parse FIRMS data' });
          });
  } catch (error) {
      console.error('Error fetching FIRMS data:', error);
      res.status(500).json({ error: 'Failed to fetch FIRMS data' });
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

  const user_id = req.body.user_id || req.query.user_id;
  const name = req.body.name || req.query.name;
  const location = req.body.location || req.query.location;
  const latitude = req.body.latitude || req.query.latitude;
  const longitude = req.body.longitude || req.query.longitude;
  const account_type = req.body.account_type || req.query.account_type;
  const email = req.body.email || req.query.email
  const regToken = req.body.regToken || req.query.regToken
  const notifications = req.body.notifications || req.query.notifications
  
  if (!name || !location || !account_type || !email || !regToken || notifications == null) {
    console.log("Bad params", req.body);
    return res.status(400).json({ error: 'Missing name, location, email, regToken, notifications, or account_type in request body' });
  }

  console.log(`latitude: ${latitude}, longitude: ${longitude}`)

  if (user_id == null) {
    user_id == uuidv4();
  }
  
  const newUser = {
    user_id: user_id,
    name,
    location,
    latitude,
    longitude,
    account_type,
    email,
    regToken,
    notifications,
    created_at: new Date().toISOString()
  };

  const params = {
    TableName: 'user', 
    Item: newUser
  };

  try {
    await dynamoDB.put(params).promise();

    /*
     const message = {
      notification: {
        title: "This is a test notification",
        body: "This is a notification sent to users when they save their profile."
      },
      token: newUser.regToken
    };
    console.log('Message object:', message);

    // Send a message to the device corresponding to the provided
    // registration token.
    getMessaging().send(message)
    .then((response) => {
      // Response is a message ID string.
      console.log('Successfully sent message:', response);
    })
    .catch((error) => {
      console.log('Error sending message:', error);
    });
    */

    notifyUsers();
    
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

app.post('/user/locations', async (req, res) => {
  const { user_id, latitude, longitude } = req.body;

  // Validate required fields
  if (!user_id || !latitude || !longitude) {
    return res.status(400).json({ error: 'Missing user_id, latitude, or longitude in request body.' });
  }

  // Define DynamoDB parameters to update the user's location
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
    // Update the user's location in the DynamoDB table
    const result = await dynamoDB.update(params).promise();
    res.status(200).json({ 
      message: 'User location updated successfully',
      updatedAttributes: result.Attributes, // Return the updated fields
    });
  } catch (error) {
    console.error('Error updating user location:', error);
    res.status(500).json({ error: 'Error updating user location' });
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

const notifyUsers = async () => {
  var events = [];
  try {
    const params = {
      TableName: 'event'
    };

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

  var users = [];
  try {
    const params = {
      TableName: 'user'
    };

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
    for (const event of events) {
      const dangerLevel = dangerLevelCalc(event.lat, event.lng, user.latitude, user.longitude, event.event_type);
      if (dangerLevel > 25) {
        console.log(`lat1: ${event.lat}, lon1: ${event.lng}, lat2: ${user.latitude}, lon2: ${user.longitude}, disaster: ${event.event_type}`)
        console.log("Danger Level: ", dangerLevel);
        const message = {
          notification: {
            title: `WARNING: ${event.event_type} detected near your location!`,
            body: `${event.event_type} detected. Lat: ${event.lat.toFixed(2)}, Lng: ${event.lng.toFixed(2)}`
          },
          token: user.regToken,
          android: {
            notification: {
              tag: `event_${event.event_id}`
            }
          }
        };

        getMessaging().send(message)
          .then((response) => {
            // Response is a message ID string.
            console.log('Successfully sent message:', response);
          })
          .catch((error) => {
            console.log('Error sending message:', error);
          });
      }
    }
  }
}

const dangerLevelCalc = function(lat1, lon1, lat2, lon2, disasterType) {

  baseDangerLevels = {
    "WF": 100,  // Wildfire
    "EQ": 80,  // Earthquake
    "FL": 75,  // Flood
    "TS": 70,  // Tropical storm
    "HU": 95,  // Hurricane
    "TO": 90,  // Tornado
    "BZ": 65,  // Blizzard
    "VO": 85,  // Volcano
    "LS": 70   // Landslide
  };

  const R = 6371000; // Radius of Earth in meters

  // Conversion from latitude and longitude to meters
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);

  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  distance = R * c; // Distance in meters

  const danger = baseDangerLevels[disasterType];

  const distanceFactor = 1.0 - Math.min(1.0, distance / 500000.0); // Max danger distance set to 500,000m, or 500km

  const scaledDanger = Math.round(danger*distanceFactor);

  return scaledDanger;

};


// Schedule the job to run at the start of every hour
// cron.schedule('0 0 * * *', fetchDisasterData);

app.listen(80, () => {
  console.log("Tempest Backend: Server running on port 80");
});
