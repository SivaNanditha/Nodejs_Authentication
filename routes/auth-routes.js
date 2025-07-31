const express = require("express");

const {
  registerUser,
  loginUser,
  changePassword,
} = require("../controllers/auth-controller");

const router = express.Router();   // organizes the code by grouping related routes together.
const authMiddleware = require("../middleware/auth-middleware");

router.post("/register/", registerUser);
router.post("/login", loginUser);
router.post("/change-password", authMiddleware, changePassword);

module.exports = router;
