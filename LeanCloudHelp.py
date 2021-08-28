#!/usr/bin/python3

import leancloud

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


def createTable():
    if (isInit == False):
        return

    # User
    USER = leancloud.Object.extend('USER')
    user = USER()
    user.set('name', 'TestUser')
    user.set('avatar', '1')
    user.save()

    # AgoraRoom
    AgoraRoom = leancloud.Object.extend('AGORA_ROOM')
    mAgoraRoom = AgoraRoom()
    mAgoraRoom.set('channelName', 'TestId')
    mAgoraRoom.set('userId', '123456')
    mAgoraRoom.set('cover', 'Test')
    mAgoraRoom.set('mv', 'Test')
    mAgoraRoom.save()

    AgoraMember = leancloud.Object.extend('MEMBER_KTV')
    mAgoraMember = AgoraMember()
    mAgoraMember.set('roomId', mAgoraRoom)
    mAgoraMember.set('userId', 'TestId')
    mAgoraMember.set('streamId', 123456)
    mAgoraMember.set('role', 1)
    mAgoraMember.set('isMuted', 0)
    mAgoraMember.set('isSelfMuted', 0)
    mAgoraMember.save()

    MusicKTV = leancloud.Object.extend('MUSIC_KTV')
    mMusicKTV = MusicKTV()
    mMusicKTV.set('name', 'Test')
    mMusicKTV.set('userId', 'Test')
    mMusicKTV.set('roomId', mAgoraRoom)
    mMusicKTV.set('musicId', 'Test')
    mMusicKTV.save()

    user.destroy()
    mAgoraRoom.destroy()
    mAgoraMember.destroy()
    mMusicKTV.destroy()
    print("Done!!")


init()
createTable()
