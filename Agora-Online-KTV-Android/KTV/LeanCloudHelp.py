#!/usr/bin/python3

import leancloud

appid = 'HlhdxEP2mt0agmaDECdzHRrP-9Nh9j0Va'
appkey = 'cuBS7GYWcGvOFfV9oBVXj5lf'

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

    # Room
    ROOM = leancloud.Object.extend('ROOM_MARRY')
    room = ROOM()
    room.set('anchorId', user)
    room.set('channelName', "TestName")
    room.save()

    # Member
    MEMBER = leancloud.Object.extend('MEMBER_MARRY')
    member = MEMBER()
    member.set('isMuted', 0)
    member.set('isSelfMuted', 0)
    member.set('role', 1)
    member.set('roomId', room)
    member.set('streamId', 10000)
    member.set("userId", user)
    member.save()

    # Action
    ACTION = leancloud.Object.extend('ACTION_MARRY')
    action = ACTION()
    action.set('action', 1)
    action.set('memberId', member)
    action.set('roomId', room)
    action.set('status', 1)
    action.save()

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

    user.destroy()
    room.destroy()
    member.destroy()
    action.destroy()
    mAgoraRoom.destroy()
    print("Done!!")


init()
createTable()
