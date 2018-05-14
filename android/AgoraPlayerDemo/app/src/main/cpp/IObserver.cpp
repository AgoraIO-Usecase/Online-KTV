//
// Created by 湛孝超 on 2018/5/4.
//

#include "IObserver.h"
//主体函数添加观察者
void IObserver::AddObs(IObserver *obs) {
    if(!obs)return;
    mux.lock();
    obss.push_back(obs);
    mux.unlock();
}
//通知所有观察者
void IObserver::Notify(XData data) {
    mux.lock();
    for (int i = 0; i < obss.size(); ++i) {
        obss[i]->Update(data);
    }
    mux.unlock();

}