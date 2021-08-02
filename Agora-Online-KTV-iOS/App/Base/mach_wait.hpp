//
//  mach_wait.hpp
//  OpenLive
//
//  Created by LK on 2021/6/13.
//  Copyright © 2021 Agora. All rights reserved.
//

#ifndef mach_wait_hpp
#define mach_wait_hpp

#include <stdio.h>
#include <pthread.h>

void move_pthread_to_realtime_scheduling_class(pthread_t pthread);
void mach_wait_until(int millisec);

#endif /* mach_wait_hpp */
