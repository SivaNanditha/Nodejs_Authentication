const mongoose = require("mongoose");

const connectToDB = async () => {
  try {
    await mongoose.connect(process.env.MONGO_URL);
    console.log("Mongo Db connected successfully");
  } catch (e) {
    console.error("Mongo DB connection failed", err);
    process.exit(1);
  }
};

module.exports = connectToDB;
