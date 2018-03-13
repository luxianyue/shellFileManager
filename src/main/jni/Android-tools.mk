LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE:=tools
LOCAL_SRC_FILES:=tools.c
include $(BUILD_EXECUTABLE)