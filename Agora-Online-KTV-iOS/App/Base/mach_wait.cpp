//
//  mach_wait.cpp
//  OpenLive
//
//  Created by LK on 2021/6/13.
//  Copyright © 2021 Agora. All rights reserved.
//

#include "mach_wait.hpp"
#include <mach/mach.h>
#include <mach/mach_time.h>

void move_pthread_to_realtime_scheduling_class(pthread_t pthread) {
    mach_timebase_info_data_t timebase_info;
    mach_timebase_info(&timebase_info);

    const uint64_t NANOS_PER_MSEC = 1000000ULL;
    double clock2abs = ((double)timebase_info.denom / (double)timebase_info.numer) * NANOS_PER_MSEC;

    thread_time_constraint_policy_data_t policy;
    policy.period      = 0;
    policy.computation = (uint32_t)(5 * clock2abs); // 5 ms of work
    policy.constraint  = (uint32_t)(10 * clock2abs);
    policy.preemptible = FALSE;

    int kr = thread_policy_set(pthread_mach_thread_np(pthread_self()),
                   THREAD_TIME_CONSTRAINT_POLICY,
                   (thread_policy_t)&policy,
                   THREAD_TIME_CONSTRAINT_POLICY_COUNT);
    if (kr != KERN_SUCCESS) {
        mach_error("thread_policy_set:", kr);
      return;
    }
}

static const uint64_t NANOS_PER_USEC = 1000ULL;
static const uint64_t NANOS_PER_MILLISEC = 1000ULL * NANOS_PER_USEC;
static const uint64_t NANOS_PER_SEC = 1000ULL * NANOS_PER_MILLISEC;

static mach_timebase_info_data_t timebase_info;

static uint64_t abs_to_nanos(uint64_t abs) {
    return abs * timebase_info.numer  / timebase_info.denom;
}

static uint64_t nanos_to_abs(uint64_t nanos) {
    return nanos * timebase_info.denom / timebase_info.numer;
}

void mach_wait_until(int millisec) {
    mach_timebase_info(&timebase_info);
    uint64_t time_to_wait = nanos_to_abs(millisec * NANOS_PER_MILLISEC);
    uint64_t now = mach_absolute_time();
    mach_wait_until(now + time_to_wait);
}
