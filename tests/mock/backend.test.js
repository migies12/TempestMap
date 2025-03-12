// tempest.test.js
const request = require('supertest');
const AWS = require('aws-sdk');
const axios = require('axios');
jest.mock('axios');

const app = require('backend/tempest'); 
const { Readable } = require('stream');


describe("POST /test_cron", () => {
    afterEach(() => {
      jest.restoreAllMocks();
    });
  
    it("should return 200 on successful disaster data fetch", async () => {
      // Ensure both axios.get calls in fetchDisasterData are mocked:
      jest.spyOn(axios, "get")
        .mockResolvedValueOnce({ data: { result: [] } })
        .mockResolvedValueOnce({ data: { result: [] } });
      const res = await request(app).post("/test_cron");
      expect(res.statusCode).toBe(200);
      expect(res.body).toEqual({ message: "Disaster data fetched successfully!" });
    });
  
    // it("should return 500 when disaster data fetch fails", async () => {
    //   // Force an error by having the first axios.get call reject.
    //   jest.spyOn(axios, "get")
    //     .mockRejectedValueOnce(new Error("Forced error"))
    //     .mockResolvedValueOnce({ data: { result: [] } });
    //   const res = await request(app).post("/test_cron");
    //   expect(res.statusCode).toBe(500);
    //   expect(res.body).toHaveProperty("error", "Error fetching disaster data");
    // });
  });


describe("GET /event", () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should return events from DynamoDB", async () => {
    const testEvent = { 
      event_id: "d853b59f-a5b1-4034-bede-5c011ec6eae6", 
      event_name: "Test Event" 
    };
    jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "scan").mockImplementationOnce(() => ({
      promise: () => Promise.resolve({
        Items: [testEvent],
        LastEvaluatedKey: null,
      }),
    }));
    
    const res = await request(app).get("/event");
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty("events");
    expect(Array.isArray(res.body.events)).toBe(true);
    // Check that event_id exists and is a string
    expect(res.body.events[0]).toHaveProperty("event_id");
    expect(typeof res.body.events[0].event_id).toBe("string");
    // Optionally, match against a UUID regex pattern
    expect(res.body.events[0].event_id).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    );
  });

});

describe("POST /event/custom", () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should return 400 if required fields are missing", async () => {
    const res = await request(app).post("/event/custom").send({});
    expect(res.statusCode).toBe(400);
    expect(res.body).toHaveProperty("error");
  });

  it("should create a custom event successfully", async () => {
    jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "put").mockImplementationOnce(() => {
      return { promise: () => Promise.resolve() };
    });
    const data = { latitude: 10.0, longitude: 20.0, markerType: "Test" };
    const res = await request(app).post("/event/custom").send(data);
    expect(res.statusCode).toBe(201);
    expect(res.body).toHaveProperty("message", "Custom event created successfully");
    expect(res.body).toHaveProperty("event");
    expect(res.body.event).toHaveProperty("lat", 10.0);
    expect(res.body.event).toHaveProperty("lng", 20.0);
  });

    //   it("should handle errors when creating a custom event", async () => {
    //     // jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "put").mockImplementationOnce(() => {
    //     //   return { promise: () => Promise.reject(new Error("Forced error")) };
    //     // });
    //     const data = { latitude: 10.0, longitude: 20.0, markerType: "Test" };
    //     const res = await request(app).post("/event/custom").send(data);
    //     expect(res.statusCode).toBe(500);
    //     expect(res.body).toHaveProperty("error", "Error creating custom event");
    //   });
});

// describe("GET /event/firms", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should fetch and parse FIRMS data successfully", async () => {
//     // Prepare a simple CSV stream.
//     const csvData = "a,b\n1,2\n";
//     const streamData = Readable.from([csvData]);
//     jest.spyOn(axios, "get").mockResolvedValue({ data: streamData });
//     const res = await request(app).get("/event/firms");
//     expect(res.statusCode).toBe(200);
//     expect(Array.isArray(res.body)).toBe(true);
//     // Expect csv-parser to have parsed the CSV correctly.
//     expect(res.body[0]).toHaveProperty("a", "1");
//     expect(res.body[0]).toHaveProperty("b", "2");
//   });

//   it("should handle errors when fetching FIRMS data", async () => {
//     jest.spyOn(axios, "get").mockRejectedValue(new Error("Forced error"));
//     const res = await request(app).get("/event/firms");
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Failed to fetch FIRMS data");
//   });
// });

// describe("POST /comment/:event_id", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should return 400 if event_id or comment is missing", async () => {
//     // When comment is missing.
//     const res = await request(app).post("/comment/123").send({});
//     expect(res.statusCode).toBe(400);
//     expect(res.body).toHaveProperty("error");
//   });

//   it("should append a comment successfully", async () => {
//     const fakeUpdated = { comments: [{ comment_id: "abc", text: "Test comment", user: "User", created_at: "date" }] };
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementation(() => {
//       return { promise: () => Promise.resolve({ Attributes: fakeUpdated }) };
//     });
//     const res = await request(app).post("/comment/123").send({ comment: "Test comment", user: "User" });
//     expect(res.statusCode).toBe(200);
//     expect(res.body).toHaveProperty("message", "Comment appended successfully");
//     expect(res.body).toHaveProperty("updatedAttributes");
//   });

//   it("should handle errors when appending a comment", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementation(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const res = await request(app).post("/comment/123").send({ comment: "Test comment", user: "User" });
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error appending comment");
//   });
// });

// describe("GET /comment/:event_id", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should return 404 if event is not found", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({}) };
//     });
//     const res = await request(app).get("/comment/123");
//     expect(res.statusCode).toBe(404);
//     expect(res.body).toHaveProperty("error", "Event not found.");
//   });

//   it("should retrieve comments successfully", async () => {
//     const fakeComments = [{ comment_id: "abc", text: "Test comment" }];
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Item: { comments: fakeComments } }) };
//     });
//     const res = await request(app).get("/comment/123");
//     expect(res.statusCode).toBe(200);
//     expect(res.body).toHaveProperty("event_id", "123");
//     expect(res.body).toHaveProperty("comments");
//     expect(Array.isArray(res.body.comments)).toBe(true);
//   });

//   it("should handle errors when retrieving comments", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const res = await request(app).get("/comment/123");
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error retrieving comments.");
//   });
// });

// describe("DELETE /comment/:event_id", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should return 400 if required parameters are missing", async () => {
//     // Missing comment_id in the request.
//     const res = await request(app).delete("/comment/123").send({});
//     expect(res.statusCode).toBe(400);
//     expect(res.body).toHaveProperty("error");
//   });

//   it("should return 404 if event is not found", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({}) };
//     });
//     const res = await request(app).delete("/comment/123").send({ comment_id: "abc" });
//     expect(res.statusCode).toBe(404);
//     expect(res.body).toHaveProperty("error", "Event not found.");
//   });

//   it("should return 404 if comment is not found", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Item: { comments: [] } }) };
//     });
//     const res = await request(app).delete("/comment/123").send({ comment_id: "abc" });
//     expect(res.statusCode).toBe(404);
//     expect(res.body).toHaveProperty("error", "Comment not found.");
//   });

//   it("should remove a comment successfully", async () => {
//     // Simulate an event with one comment and a successful update.
//     const fakeComments = [{ comment_id: "abc", text: "Test" }];
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Item: { comments: fakeComments } }) };
//     });
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Attributes: { comments: [] } }) };
//     });
//     const res = await request(app).delete("/comment/123").send({ comment_id: "abc" });
//     expect(res.statusCode).toBe(200);
//     expect(res.body).toHaveProperty("message", "Comment removed successfully.");
//     expect(res.body).toHaveProperty("updatedAttributes");
//   });

//   it("should handle errors when removing a comment", async () => {
//     const fakeComments = [{ comment_id: "abc", text: "Test" }];
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Item: { comments: fakeComments } }) };
//     });
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementationOnce(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const res = await request(app).delete("/comment/123").send({ comment_id: "abc" });
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error removing comment.");
//   });
// });

// describe("POST /user", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should return 400 if required fields are missing", async () => {
//     const res = await request(app).post("/user").send({ name: "Test" });
//     expect(res.statusCode).toBe(400);
//     expect(res.body).toHaveProperty("error");
//   });

//   it("should create a user successfully", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "put").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve() };
//     });
//     const data = {
//       name: "Test User",
//       location: "Test Location",
//       account_type: "standard",
//       email: "test@example.com",
//       regToken: "sometoken",
//       notifications: true,
//       latitude: 10.0,
//       longitude: 20.0
//     };
//     const res = await request(app).post("/user").send(data);
//     expect(res.statusCode).toBe(201);
//     expect(res.body).toHaveProperty("message", "User created successfully");
//     expect(res.body).toHaveProperty("user");
//     expect(res.body.user).toHaveProperty("name", data.name);
//   });

//   it("should handle errors when creating a user", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "put").mockImplementationOnce(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const data = {
//       name: "Test User",
//       location: "Test Location",
//       account_type: "standard",
//       email: "test@example.com",
//       regToken: "sometoken",
//       notifications: true,
//       latitude: 10.0,
//       longitude: 20.0
//     };
//     const res = await request(app).post("/user").send(data);
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error creating user");
//   });
// });

// describe("GET /user/:user_id", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should retrieve a user successfully", async () => {
//     const fakeUser = { user_id: "123", name: "Test User" };
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Item: fakeUser }) };
//     });
//     const res = await request(app).get("/user/123");
//     expect(res.statusCode).toBe(200);
//     expect(res.body).toHaveProperty("user");
//     expect(res.body.user).toEqual(fakeUser);
//   });

//   it("should return 404 if the user is not found", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({}) };
//     });
//     const res = await request(app).get("/user/123");
//     expect(res.statusCode).toBe(404);
//     expect(res.body).toHaveProperty("error", "User not found.");
//   });

//   it("should handle errors when retrieving a user", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "get").mockImplementationOnce(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const res = await request(app).get("/user/123");
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error retrieving user");
//   });
// });

// describe("POST /user/locations", () => {
//   afterEach(() => {
//     jest.restoreAllMocks();
//   });

//   it("should return 400 if required fields are missing", async () => {
//     const res = await request(app).post("/user/locations").send({});
//     expect(res.statusCode).toBe(400);
//     expect(res.body).toHaveProperty("error");
//   });

//   it("should update a user's location successfully", async () => {
//     const fakeUpdated = { latitude: 15.0, longitude: 25.0 };
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementationOnce(() => {
//       return { promise: () => Promise.resolve({ Attributes: fakeUpdated }) };
//     });
//     const data = { user_id: "123", latitude: 15.0, longitude: 25.0 };
//     const res = await request(app).post("/user/locations").send(data);
//     expect(res.statusCode).toBe(200);
//     expect(res.body).toHaveProperty("message", "User location updated successfully");
//     expect(res.body).toHaveProperty("updatedAttributes");
//     expect(res.body.updatedAttributes).toEqual(fakeUpdated);
//   });

//   it("should handle errors when updating a user's location", async () => {
//     jest.spyOn(AWS.DynamoDB.DocumentClient.prototype, "update").mockImplementationOnce(() => {
//       return { promise: () => Promise.reject(new Error("Forced error")) };
//     });
//     const data = { user_id: "123", latitude: 15.0, longitude: 25.0 };
//     const res = await request(app).post("/user/locations").send(data);
//     expect(res.statusCode).toBe(500);
//     expect(res.body).toHaveProperty("error", "Error updating user location");
//   });
// });
