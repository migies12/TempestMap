const express = require('express');
const AWS = require('aws-sdk');
const cron = require('node-cron');

// Setup
const app = express();
app.use(express.json()); // Middleware to parse JSON request bodies

// AWS
AWS.config.update({ region: 'us-east-1' });
const dynamoDB = new AWS.DynamoDB.DocumentClient();

// GET /event
app.post('/event', (req, res) => {

  const { id, value } = req.body;

  const params = {
    TableName: 'tempest', // Replace with your table name
    Item: {
      id: id,               // Primary key value; ensure this attribute matches your table's key schema
      value: value,         // Other data you want to store
      createdAt: new Date().toISOString() // Optionally add a timestamp
    },
  };

  // Insert the item into DynamoDB
  dynamoDB.put(params, (err, data) => {
    if (err) {
      console.error("Error inserting data:", err);
      return res.status(500).json({ error: "Error inserting data", details: err });
    } else {
      console.log("Data inserted successfully:", data);
      return res.status(200).json({ message: "Data inserted successfully" });
    }
  });
});

// CRON Job for Populating Events
cron.schedule('0 * * * *', async () => {
    try {
      const response = await axios.get('https://api.ambeedata.com/disasters/latest/by-continent', {
        params: {
          continent: 'NAR',
          page: 1,
          limit: 50,
        },
        headers: {
          'x-api-key': 'e8105bb1cae34fcca48f26e7e72b4299c08c72202b8e4255227565c3866a5644',
        },
      });
      console.log('Weather/Disaster data retrieved:', response.data);
      
    } catch (error) {
      console.error('Error fetching weather/disaster data:', error);
    }
  });

app.listen(3000, () => {
  console.log("Server running on port 3000");
});
