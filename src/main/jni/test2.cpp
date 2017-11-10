#include <stdio.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

static char* cmd(const char* command);

int main(int argc, char* argv[])
{
    //char str[20]={"0"};
    char* result = cmd(argv[1]);
    //通过该方法可以将char*转换为char数组
    printf("The result:%s\n",result);
    return 0;
}

static char* cmd(const char* command)
{
    FILE *file = popen(command, "r");
    if (file == NULL) {
        printf("cmd popen failure\n");
        return "failure";
    }
    printf("cmd popen successful, uid=%d, gid=%d, pid=%d\n", getuid(), getgid(), getpid());
    char buf[1024];
    memset(buf, 0, sizeof(buf));
    if (strcmp("su", command) == 0) {
        return "su";
    }
    while(fgets(buf, sizeof(buf), file) != NULL) {
        printf("cmd: %s\n", buf);
    }
    if(file != NULL)
        pclose(file);
    return "success";
}