/*
 * Copyright (C) 2012 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android.camera.open;

import android.hardware.Camera;
import com.google.zxing.client.android.CaptureActivity;

/**
 * Default implementation for Android before API 9 / Gingerbread.
 */
final class DefaultOpenCameraInterface implements OpenCameraInterface {

    /**
     * Opens the device camera; prefers a back-facing camera but falls back to a front-facing one.
     *
     * @return null if the device has no camera
     */
    @Override
    public Camera open() {
        int preferredId = CaptureActivity.preferredCameraId();
        return preferredId >= 0 ? Camera.open(preferredId) : null;

//        // tries to open the back-facing camera, if any
//        Camera backFacing = Camera.open();
//        if (backFacing != null) {
//            return backFacing;
//        } else if (Camera.getNumberOfCameras() > 0) {
//            // attempts to open the front-facing camera
//            Camera frontFacing = Camera.open(0);
//            // the preview is upside down by default on Fire HDX 7" which only has a front-facing camera; this should fix it
////            frontFacing.setDisplayOrientation(180);
//            return frontFacing;
//        } else {
//            return null;
//        }
    }

}
