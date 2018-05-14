//
// Created by 湛孝超 on 2018/5/4.
//

#include "IDemux.h"
void IDemux::Main() {
    while(!isExit)
    {
        if(IsPause())
        {
            XSleep(2);
            continue;
        }
        XData d = Read();
        if (d.size > 0 )
        {
            Notify(d);
        } else{
            XSleep(2);
        }

    }


}