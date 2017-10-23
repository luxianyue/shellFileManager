LOCAL_PATH:=$(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE:=myls
LOCAL_SRC_FILES:=myls.c
include $(BUILD_EXECUTABLE)