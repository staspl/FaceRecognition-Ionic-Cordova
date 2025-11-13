//
//  FaceDetectionParam.h
//  FacePlugin
//
//  Face Detection Parameters configuration class
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface FaceDetectionParam : NSObject

@property (nonatomic, assign) BOOL check_liveness;
@property (nonatomic, assign) BOOL check_mouth_opened;
@property (nonatomic, assign) BOOL check_eye_closeness;
@property (nonatomic, assign) BOOL check_face_occlusion;
@property (nonatomic, assign) int check_liveness_level;

@end

NS_ASSUME_NONNULL_END

