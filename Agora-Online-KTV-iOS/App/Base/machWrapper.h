//
//  machWrapper.h
//  OpenLive
//
//  Created by LK on 2021/6/13.
//  Copyright © 2021 Agora. All rights reserved.
//

#ifndef machWrapper_h
#define machWrapper_h

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface MachWrapper : NSObject

+(void)wait:(int) ms;

+(time_t)uptime;

@end
#endif /* machWrapper_h */
