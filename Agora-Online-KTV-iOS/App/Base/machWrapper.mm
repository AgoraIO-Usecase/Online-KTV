//
//  machWrapper.m
//  OpenLive
//
//  Created by LK on 2021/6/13.
//  Copyright © 2021 Agora. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "machWrapper.h"
#import "mach_wait.hpp"
#import <UIKit/UIKit.h>
#include <sys/sysctl.h>

@implementation MachWrapper

+(void)wait:(int) ms
{
  mach_wait_until(ms);
}

+(time_t)uptime
{
    struct timeval boottime;
    int mib[2] = {CTL_KERN, KERN_BOOTTIME};
    size_t size = sizeof(boottime);
    time_t now;
    time_t uptime = -1;

    (void)time(&now);

    if (sysctl(mib, 2, &boottime, &size, NULL, 0) != -1 && boottime.tv_sec != 0) {
        uptime = now - boottime.tv_sec;
    }
    return uptime;
}

@end
