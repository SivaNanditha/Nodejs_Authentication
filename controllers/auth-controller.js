const User = require("../models/User");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const registerUser = async (req, res) => {
  try {
    //extract user information from user request body
    const { username, email, password, role } = req.body;
    //check if user already exists
    const existingUser = await User.findOne({ $or: [{ username }, { email }] });
    //{$or : [{username}, {email}]}  checks whether username and email already exists in db or not

    if (existingUser) {
      return res.status(400).json({
        success: false,
        message: "User already exists either with same username or email",
      });
    }
    //hash the user password

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);

    //create new user

    const newlyCreatedUser = new User({
      username,
      email,
      password: hashedPassword,
      role: role || "user",
    });

    await newlyCreatedUser.save();

    if (newlyCreatedUser) {
      res.status(201).json({
        success: true,
        message: "User created successfully",
      });
    } else {
      res.status(400).json({
        success: false,
        message: "Unable to register the user please try again later",
      });
    }
  } catch (e) {
    console.log(e);
    res.status(500).json({
      success: false,
      message: "Something went wrong",
    });
  }
};

const loginUser = async (req, res) => {
  try {
    const { username, password } = req.body;
    //find currentUser exists in db or not

    const user = await User.findOne({ username });
    if (!user) {
      return res.status(400).json({
        success: false,
        message: "User doesn't exists",
      });
    }

    //check password is crct or not

    const isPasswordMatch = await bcrypt.compare(password, user.password);
    if (!isPasswordMatch) {
      return res.status(400).json({
        success: false,
        message: "Invalid credentials",
      });
    }

    //create user token
    const accessToken = jwt.sign(
      {
        userId: user._id,
        username: user.username,
        role: user.role,
      },
      process.env.JWT_SECRET_KEY,
      {
        expiresIn: "30m",
      }
    );

    res.status(200).json({
      success: true,
      message: "Logged in successfully",
      accessToken,
    });
  } catch (e) {
    console.log(e);
    res.status(500).json({
      success: false,
      message: "Something went wrong",
    });
  }
};

const changePassword = async (req, res) => {
  try {
    const userId = req.userInfo.userId; //only logged in user is able to change the password, so logged in user info is available in userInfo which comes from auth-middleware
    //extract old and new password

    const { oldPassword, newPassword } = req.body;
    //find the current logged in user
    const user = await User.findById(userId);
    if (!user) {
      return res.status(400).json({
        success: false,
        message: "User not found",
      });
    }
    //check if old password is correct
    const isPasswordMatch = await bcrypt.compare(oldPassword, user.password);
    if (!isPasswordMatch) {
      return res.status(400).json({
        success: false,
        message: "Old password is not correct",
      });
    }

    //hash the new password

    const salt = await bcrypt.genSalt(10);
    const newHashedPassword = await bcrypt.hash(newPassword, salt);

    //update the user password
    user.password = newHashedPassword;
    await user.save();

    return res.status(200).json({
      success: true,
      message: "Password changed successfully",
    });
  } catch (e) {
    console.log(e);
    res.status(500).json({
      success: false,
      message: "Something went wrong",
    });
  }
};

module.exports = { registerUser, loginUser, changePassword };
