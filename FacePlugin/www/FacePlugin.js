var exec = require('cordova/exec');

function set_activation(license, onsuccess, onerror ) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "set_activation", [{"license":license}]);
};

function test(onsuccess, onerror ) {
    exec(onsuccess, onerror, "FacePlugin", "test", []);
};

function faces_get(onsuccess, onerror ) {
    exec(onsuccess, onerror, "FacePlugin", "faces_get", []);
};


function face_register(cam_id, onsuccess, onerror) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "face_register", [{cam_id:cam_id}]);
};


function face_recognize(cam_id, onsuccess, onerror) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "face_recognize", [{cam_id:cam_id }]);
};

function update_data(user_list, onsuccess, onerror) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "update_data", [{user_list: user_list}]);
};

function close_camera(onsuccess, onerror) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "close_camera", []);    
};

function clear_data(onsuccess, onerror) {
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "clear_data", []);    
};

function face_register_from_image(face_id, image, onsuccess, onerror, showToast, threshold) {
// alert("Test1: " + face_id + ":" + image);	
    exec(onsuccess, onerror ?? function(err){}, "FacePlugin", "face_register_from_image", [{"image": image, "face_id": face_id ?? "", "showToast":showToast, "threshold":threshold ?? 0.8 }]);
// alert("Test2");	
};

module.exports = {
    face_register: face_register,
    face_recognize: face_recognize,
    update_data: update_data,
    close_camera: close_camera,
    clear_data: clear_data,
    set_activation,
    test,
    faces_get,
    face_register_from_image
};