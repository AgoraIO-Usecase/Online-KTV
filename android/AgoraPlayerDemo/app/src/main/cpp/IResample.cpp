//
// Created by 湛孝超 on 2018/5/4.
//

#include "IResample.h"
void IResample::Update(XData data) {
    XData d = this->Resample(data);
    if(d.size > 0){
        this->Notify(d);
    }
}