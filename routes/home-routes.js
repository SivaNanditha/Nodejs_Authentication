const express = require("express");

const router = express.Router();

const authMiddleware = require("../middleware/auth-middleware");

// router.get("/welcome", (req, res) => {
//   res.json({
//     message: "Welcome to the Home Page",
//   });
// });   here we can add  multiple handlers for example

// router.get("/welcome", handler1, handler2, (req, res) => {
//here what we are specifying is frst go to /welcome then check handler1 if its success go to handler2 if its success then execute that function

router.get("/welcome", authMiddleware, (req, res) => {
  const { username, userId, role } = req.userInfo;
  res.json({
    message: "Welcome to the Home Page",
    user: {
      _id: userId,
      username,
      role,
    },
  });
});

module.exports = router;
