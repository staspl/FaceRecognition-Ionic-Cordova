//
//  FaceDetectionParam.m
//  FacePlugin
//
//  Face Detection Parameters configuration class
//

#import "FaceDetectionParam.h"

@implementation FaceDetectionParam

- (instancetype)init {
    self = [super init];
    if (self) {
        _check_liveness = NO;
        _check_mouth_opened = NO;
        _check_eye_closeness = NO;
        _check_face_occlusion = NO;
        _check_liveness_level = 0;
    }
    return self;
}

@end

