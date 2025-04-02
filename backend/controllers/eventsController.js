const dynamoDB = require('../config/aws');
const axios = require('axios');
const csv = require('csv-parser');

exports.getEvents = async (req, res) => {
  try {
    const params = { TableName: 'event' };
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
};

exports.getFirmsData = async (req, res) => {
  console.log('Received request at /event/firms');
  try {
    const currentDate = new Date().toISOString().split('T')[0];
    const url = `https://firms.modaps.eosdis.nasa.gov/api/area/csv/840168c717a27d2e1ed7faf3744dc8cc/VIIRS_NOAA21_NRT/world/10/${currentDate}`;
    console.log('Fetching FIRMS data from:', url);

    const response = await axios.get(url, { responseType: 'stream' });
    const results = [];

    response.data
      .pipe(csv())
      .on('data', (data) => results.push(data))
      .on('end', () => {
        console.log('FIRMS data fetched and parsed successfully:', results);
        res.json(results);
      })
      .on('error', (error) => {
        console.error('Error parsing CSV:', error);
        res.status(500).json({ error: 'Failed to parse FIRMS data' });
      });
  } catch (error) {
    console.error('Error fetching FIRMS data:', error);
    res.status(500).json({ error: 'Failed to fetch FIRMS data' });
  }
};
