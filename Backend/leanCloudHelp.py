#!/usr/bin/python3

import leancloud
from leancloud import cloud

appid = ''
appkey = ''

isInit = False


def init():
    global isInit
    isInit = False
    if appid == None or len(appid) <= 0:
        print("appid is empty")
        return

    if appkey == None or len(appkey) <= 0:
        print("appkey is empty")
        return

    leancloud.init(appid, appkey)
    isInit = True


def getList():
    MusicRepo = leancloud.Object.extend('MUSIC_REPOSITORY')
    query = MusicRepo.query
    list = query.find()
    for music in list:
        print(music.get('musicId'), music.get('name'))


def runFun():
    print("lrc url:", cloud.run('getLrc', id='001'), " music url:", cloud.run('getMusic', id='001'))


def insertMusics():
    if (isInit == False):
        return

    # User
    MusicRepo = leancloud.Object.extend('MUSIC_REPOSITORY')
    musicRepo = MusicRepo()
    musicRepo.set('musicId', '001')
    musicRepo.set('name', '七里香')
    musicRepo.save()
    musicRepo = MusicRepo()
    musicRepo.set('musicId', '002')
    musicRepo.set('name', '十年')
    musicRepo.save()
    musicRepo = MusicRepo()
    musicRepo.set('musicId', '003')
    musicRepo.set('name', '后来')
    musicRepo.save()
    musicRepo = MusicRepo()
    musicRepo.set('musicId', '004')
    musicRepo.set('name', '我怀念的')
    musicRepo.save()
    musicRepo = MusicRepo()
    musicRepo.set('musicId', '005')
    musicRepo.set('name', '突然好想你')
    musicRepo.save()

    print("数据库创建成功。")


init()
getList()
runFun()
# insertMusics()
