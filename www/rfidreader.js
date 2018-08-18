var exec = require('cordova/exec');

exports.Test = function (arg0, success, error) {
    exec(success, error, 'rfidreader', 'Test', [arg0]);
};

exports.ReadCardSN = function (device,baudrate, success, error) {
    exec(success, error, 'rfidreader', 'ReadCardSN', [device,baudrate]);
};
exports.ReadCardData = function (device,baudrate,block,strPWS, success, error) {
    exec(success, error, 'rfidreader', 'ReadCardData', [device,baudrate,block,strPWS]);
};
exports.ReadCardEsquelSN = function (device,baudrate, success, error) {
    exec(success, error, 'rfidreader', 'ReadCardEsquelSN', [device,baudrate]);
};