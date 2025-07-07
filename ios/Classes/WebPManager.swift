//
// Copyright (c) WhatsApp Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.
//

import UIKit
import SDWebImageWebPCoder

class WebPManager {

    static let shared: WebPManager = WebPManager()

    func isAnimated(webPData data: Data) -> Bool {
        guard let duration = self.decode(webPData: data)?.duration else {
            return false
        }
        
        return duration > 0
    }
    
    func decode(webPData data: Data) -> UIImage? {
        return SDImageWebPCoder.shared.decodedImage(with: data, options: nil)
    }

    func encode(pngData data: Data) -> Data? {
        return SDImageWebPCoder.shared.encodedData(with: UIImage(data: data), format: .webP)
    }
    
    func minFrameDuration(webPData data: Data) -> TimeInterval? {
        guard let image = decode(webPData: data), isAnimated(webPData: data) else {
            return nil
        }
        
        // Get frame durations from animated image
        if let animatedImage = image as? SDAnimatedImage {
            var minDuration: TimeInterval = Double.greatestFiniteMagnitude
            
            for i in 0..<animatedImage.animatedImageFrameCount {
                let duration = animatedImage.animatedImageDuration(at: i)
                if duration < minDuration {
                    minDuration = duration
                }
            }
            
            return minDuration == Double.greatestFiniteMagnitude ? nil : minDuration
        }
        
        return nil
    }
    
    func totalAnimationDuration(webPData data: Data) -> TimeInterval? {
        guard let image = decode(webPData: data), isAnimated(webPData: data) else {
            return nil
        }
        
        return image.duration
    }
}
