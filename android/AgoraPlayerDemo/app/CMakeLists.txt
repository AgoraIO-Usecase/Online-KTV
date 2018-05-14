# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#添加头文件路径
include_directories(include)

#设置ffmpeg所在库的路径
set(FF ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${FF}/libavcodec.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${FF}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${FF}/libavutil.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${FF}/libswscale.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${FF}/libswresample.so)

add_library(libyuv STATIC IMPORTED)
set_target_properties(libyuv PROPERTIES IMPORTED_LOCATION ${FF}/libyuv.a)


add_library( # Sets the name of the library.

             apm-plugin-native-lib
             # Sets the library as a shared library.

             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/apm-plugin-native-lib.cpp
             src/main/cpp/AgoraFFDemux.cpp
             src/main/cpp/IDemux.cpp
             src/main/cpp/IObserver.cpp
             src/main/cpp/XData.cpp
             src/main/cpp/XThread.cpp
             src/main/cpp/AgoraLog.cpp
             src/main/cpp/AgoraParameter.cpp
             src/main/cpp/AgoraFFDecode.cpp
             src/main/cpp/IDecode.cpp
             src/main/cpp/AgoraFFResample.cpp
             src/main/cpp/IResample.cpp
             src/main/cpp/IAudioPlay.cpp
             src/main/cpp/IVideoView.cpp
             src/main/cpp/GLVideoView.cpp
             src/main/cpp/XTexture.cpp
             src/main/cpp/XShader.cpp
             src/main/cpp/XEGL.cpp
             src/main/cpp/IPlayer.cpp
             src/main/cpp/IPlayerBuilder.cpp
             src/main/cpp/SLAudioPlay.cpp
             src/main/cpp/AgoraFFPlayerBuilder.cpp
             src/main/cpp/AgoraIPayerProxy.cpp

              )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       apm-plugin-native-lib
                       GLESv2 EGL
                       OpenSLES
                       android
                       avcodec avformat avutil swscale swresample
                       libyuv
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )