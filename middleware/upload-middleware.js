const multer = require("multer");
const path = require("path");

//set multer storage

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, "uploads/");
  },
  filename: function (req, file, cb) {
    cb(
      null,
      file.fieldname + "-" + Date.now() + path.extname(file.originalname)
    );
  },
});

//file filter function

const checkFileFilter = (req, file, cb) => {
  if (file.mimetype.startsWith("image")) {  //mime allows the types that we mention
    cb(null, true);
  } else {
    cb(new Error("Not an image! Please upload only images."));
  }
};

//multer middleware
// multer(opts)  -> a method we can pass diff options to it like storage, filefilter, limits

module.exports = multer({
  storage: storage,
  fileFilter: checkFileFilter,
  limits: {
    fileSize: 5 * 1024 * 1024, //5mb
  },
});
