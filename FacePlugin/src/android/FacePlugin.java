package com.ttv.face.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.kbyai.facesdk.*;
import com.ttv.facerecog.*;
import android.widget.Toast;
import android.app.Activity;

import java.util.HashMap;
import java.util.Map;
import java.io.PrintWriter;
import java.io.StringWriter;

import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Base64;
import org.json.JSONObject;

import java.util.Map;
import java.util.List;
import java.io.ByteArrayOutputStream;


public class FacePlugin extends CordovaPlugin {

  public static CallbackContext callbackContext;
  public static int closeCamera;
  private final float THRESHOLD_REGISTER = 0.78f;
  private boolean initialized;

  static String GetExceiptionText(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();

    Log.e("TestEngine", "action: " + action);

    if (this.initialized == false) {
      this.initialized = true;
    }

    if (action.equals("test")) {
      callbackContext.success("looks good");
      return true;
    }

    try {
      if (action.equals("faces_get")) {
        try {
          JSONArray userList = serializeUserListsToJSON();
          callbackContext.success(userList);
        } catch (Exception e) {
          Log.e("TestEngine", "Error serializing user lists: " + e.getMessage());
          callbackContext.error("Error serializing user lists");
        }
        return true;
      }

      if (action.equals("face_register")) {
        JSONObject input = args.getJSONObject(0);
        int cam_id = input.getInt("cam_id");
        FacePlugin.closeCamera = 0;

        this.openNewActivity(context, callbackContext, 0, cam_id, 10000);
        return true;
      } else if (action.equals("face_recognize")) {
        JSONObject input = args.getJSONObject(0);
        int cam_id = input.getInt("cam_id");
        FacePlugin.closeCamera = 0;
        this.openNewActivity(context, callbackContext, 1, cam_id, 10001);
        return true;
      } else if (action.equals("update_data")) {
        JSONObject input = args.getJSONObject(0);
        JSONArray user_list = input.getJSONArray("user_list");
        Log.d("TestEngine", "Update users: " + user_list.length());

        for (int i = 0; i < user_list.length(); i++) {
          JSONObject user = user_list.getJSONObject(i);
          String face_id = user.getString("face_id");
          byte[] feat = Base64.decode(user.getString("data"), Base64.DEFAULT);
          Log.d("TestEngine", " face_id: " + face_id + ": " + feat.length + " bytes");
          CameraActivity.userLists.put(face_id, feat);
        }
        callbackContext.success(user_list.length() + " faces saved");
        return true;
      } else if (action.equals("clear_data")) {

        Log.e("TestEngine", "clear_data ");
        CameraActivity.userLists.clear();
        return true;
      } else if (action.equals("close_camera")) {
        Log.e("TestEngine", "close camera");
        FacePlugin.closeCamera = 1;
        return true;
      } else if (action.equals("set_activation")) {
        JSONObject input = args.getJSONObject(0);
        String license = input.getString("license");
        int ret = FaceSDK.setActivation(license);
        if (ret == 0) {
          FaceSDK.init(cordova.getActivity().getApplicationContext().getAssets());
        }

        Log.e("TestEngine", "set activation: " + ret + " license: " + license.substring(0, 10) + "..." );

        String applicationId = cordova.getActivity().getApplicationContext().getPackageName();
        Log.e("TestEngine", "applicationId: " + applicationId);

        callbackContext.success("result: ret:" + ret + ", applicationId:" + applicationId + ", license:" + license);
      } else if (action.equals("face_register_from_image")) {
        JSONObject input = args.getJSONObject(0);
        Log.d("TestEngine", input.toString());
        String imageBase64 = input.getString("image");
        String face_id = input.getString("face_id");

        boolean showToast = true;
        try {
            showToast = input.getBoolean("showToast");
        } catch (java.lang.Exception e) {
        }

        float threshold = THRESHOLD_REGISTER;
        try {
          threshold = (float)input.getDouble("threshold");
        } catch (java.lang.Exception e) {
        }

        // Decode Base64 image into byte[]
        byte[] imageBytes = Base64.decode(imageBase64.replace("data:image/jpeg;base64,", ""), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        FaceDetectionParam faceDetectionParam = new FaceDetectionParam();
        faceDetectionParam.check_liveness = true;
        faceDetectionParam.check_liveness_level = 0;
        List<FaceBox> faceResults = FaceSDK.faceDetection(bitmap, faceDetectionParam);

        if (faceResults == null || faceResults.size() == 0) {
          callbackContext.error("No face detected in: " + imageBase64.substring (0, 10) + "...." );
          return true;
        }

        if (faceResults.size() == 1) {
          FaceBox faceBox = faceResults.get(0);
          byte[] templates = FaceSDK.templateExtraction(bitmap, faceBox);

          Rect cropRect = CameraActivity.getBestRect(bitmap.getWidth(), bitmap.getHeight(),
            new Rect(faceBox.x1, faceBox.y1, faceBox.x2, faceBox.y2));

          Bitmap cropBitmap = CameraActivity.crop(bitmap, cropRect.left, cropRect.top,
            cropRect.right - cropRect.left, cropRect.bottom - cropRect.top, 120, 120);

          ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
          cropBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
          byte[] byteArray = byteArrayOutputStream.toByteArray();
          String encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
          String encodedFeatrure = Base64.encodeToString(templates, Base64.DEFAULT);

          String maxScoreID = "";
          float maxScore = 0;
          String existsID = "";
          for (Map.Entry<String, byte[]> entry : CameraActivity.userLists.entrySet()) {
            float similarity = FaceSDK.similarityCalculation(entry.getValue(), templates);
            if (maxScore < similarity) {
              maxScore = similarity;
              maxScoreID = entry.getKey();
            }
          }

          if (maxScore > threshold ) {
            existsID = maxScoreID;
          }

          try {
            JSONObject jo = new JSONObject();
            jo.put("data", encodedFeatrure);
            jo.put("image", encodedImage);
            jo.put("exists", existsID);
            jo.put("face_id", face_id);
            jo.put("maxScore", maxScore);
            jo.put("threshold", threshold);
            CameraActivity.userLists.put(face_id, byteArray);
            callbackContext.success(jo);
          } catch (Exception e) {
            callbackContext.error("JSON error: " + e.getMessage());
          }

          if ( showToast ) {
            if (existsID.length() == 0)
              Toast.makeText(this.cordova.getActivity(), "register success!", Toast.LENGTH_SHORT).show();
            else
              Toast.makeText(this.cordova.getActivity(), "duplicated user!", Toast.LENGTH_SHORT).show();
          }

          cordova.setActivityResultCallback(null);

          return true;
        }

      }
    } catch (Throwable e) {
      String info = GetExceiptionText(e);
      Log.e("TestEngine", action + ":" + info);
      callbackContext.error(info);
      return true;
    }
    return false;
  }

  private JSONArray serializeUserListsToJSON() throws JSONException {
    JSONArray userList = new JSONArray();

    for (Map.Entry<String, byte[]> entry : CameraActivity.userLists.entrySet()) {
      JSONObject user = new JSONObject();
      user.put("face_id", entry.getKey());

      // Convert byte array to Base64 string
      String base64Data = Base64.encodeToString(entry.getValue(), Base64.DEFAULT);
      user.put("dataLength", base64Data.length());
      Log.d("TestEngine", "face_id: " + entry.getKey() + ": " + base64Data);
      userList.put(user);
    }

    return userList;
  }

  private void openNewActivity(Context context, CallbackContext callbackContext, int mode, int cam_id,
                               int rqquest_code) {

    FacePlugin.callbackContext = callbackContext;

    Intent intent = new Intent(context, CameraActivity.class);
    intent.putExtra("mode", mode);
    intent.putExtra("cam_id", cam_id);

    cordova.setActivityResultCallback(this);
    this.cordova.getActivity().startActivityForResult(intent, rqquest_code);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 10000) {
      if (resultCode == Activity.RESULT_OK) {
        String feature = intent.getStringExtra("data");
        String image = intent.getStringExtra("image");
        String existsID = intent.getStringExtra("exists");
        int featureLen = image.length();

        try {
          JSONObject jo = new JSONObject();
          jo.put("data", feature);
          jo.put("image", image);
          jo.put("exists", existsID);
          FacePlugin.callbackContext.success(jo);
        } catch (Exception e) {
        }

        if (existsID.length() == 0)
          Toast.makeText(this.cordova.getActivity(), "register success!", Toast.LENGTH_SHORT).show();
        else
          Toast.makeText(this.cordova.getActivity(), "duplicated user!", Toast.LENGTH_SHORT).show();
      } else {
        FacePlugin.callbackContext.error("register canceled!");
        Toast.makeText(this.cordova.getActivity(), "register cannceled", Toast.LENGTH_SHORT).show();
      }

      cordova.setActivityResultCallback(null);
    } else if (requestCode == 10001) {
      // if(resultCode == Activity.RESULT_OK) {
      //     // String faceID = intent.getStringExtra("face_id");
      //     // int mask = intent.getIntExtra("mask", 0);
      //     // int liveness = intent.getIntExtra("liveness", 0);

      //     // PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "WHAT");
      //     // pluginResult.setKeepCallback(true);
      //     // FacePlugin.callbackContext.sendPluginResult(pluginResult);

      //     // try {
      //     //     JSONObject jo = new JSONObject();
      //     //     jo.put("face_id", faceID);
      //     //     jo.put("mask", mask);
      //     //     jo.put("liveness", liveness);
      //     //     this.callbackContext.success(jo);
      //     // } catch(Exception e) {}

      //     // Toast.makeText(this.cordova.getActivity(), "recognize success!",  Toast.LENGTH_SHORT).show();
      // } else {
      //     // FacePlugin.callbackContext.error("register canceled!");
      //     Toast.makeText(this.cordova.getActivity(), "recognize cannceled",  Toast.LENGTH_SHORT).show();
      // }

      FacePlugin.callbackContext.error("recognize canceled!");
      cordova.setActivityResultCallback(null);
    }
  }
}

