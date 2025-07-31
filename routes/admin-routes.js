const express = require("express");

const router = express.Router();

const adminMiddleware = require("../middleware/admin-middleware");

const authMiddleware = require("../middleware/auth-middleware");

router.get("/welcome", authMiddleware, adminMiddleware, (req, res) => {
  res.json({
    message: "Welcome to the Admin Page",
  });
});

module.exports = router;

//first authMiddleware will be called so in req userInfo will be there for adminMiddleware
