#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <dirent.h>
#include <string.h>

#include <sys/stat.h>
#include <unistd.h>
#include <time.h>

#include <pwd.h>
#include <grp.h>

#include <sys/queue.h>

#include <errno.h>

#define MAX_PATH 256
const mode_t mode_p[] = {S_IRUSR, S_IWUSR, S_IXUSR, S_IRGRP, S_IWGRP, S_IXGRP, S_IROTH, S_IWOTH, S_IXOTH, S_ISUID, S_ISGID, S_ISVTX};
const char rwx[] = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x', 's', 'S', 't', 'T'};

static void get_file_permission(unsigned mode, char* per);
static char get_file_type(unsigned mode);
static void group2gid(unsigned gid, char* out);
static void user2uid(unsigned uid, char* out);
static void ls_file_info(char *path, char* name);
static long get_file_count(char* dir);


/*
 * list file
 */
static void ls_file(char *file_path) {
    char temp[MAX_PATH];
    char path[MAX_PATH];
    //realpath(file_path, temp);
    struct dirent *dp;
    DIR *dir;
    if ((dir = opendir(file_path)) == NULL) {
        perror("error");
        return;
    }
    //if (strcmp(temp, "/") != 0) {
     //   strcat(temp, "/");
    //}

    if (file_path[0] != '/') {
        char *cwd = getcwd(NULL, 0);
        if (strcmp(cwd, "/") == 0) {
            sprintf(temp, "%s", cwd);
        } else {
            sprintf(temp, "%s%s", cwd, "/");
        }
    } else {
        if (file_path[strlen(file_path) - 1] != '/') {
            sprintf(temp, "%s%s", file_path, "/");
        } else {
            sprintf(temp, "%s", file_path);
        }
    }
    while ((dp = readdir(dir)) != NULL) {
        if ((strcmp(dp->d_name, ".") == 0) || (strcmp(dp->d_name, "..") == 0)) {
            continue; //skip self and parent
        }
        sprintf(path, "%s%s", temp, dp->d_name);
        ls_file_info(path, dp->d_name);
    }
    closedir(dir);
}

/*
 * list file info
 */
static void ls_file_info(char *path, char* name) {
    struct stat st;
    if (lstat(path, &st) == -1) {
        perror("stat");
        return;
    }

    //user and group
    char user[16];
    char group[16];
    user2uid(st.st_uid, user);
    group2gid(st.st_gid, group);

    //date and time
    /*char str_time[17];
    time_t te = st.st_mtime;
    struct tm *t = localtime(&te);
    strftime(str_time, sizeof(str_time), "%Y-%m-%d %H:%M", t);
     */

    //permission and type of file
    char per_ty[12];
    per_ty[0] = get_file_type(st.st_mode);
    get_file_permission(st.st_mode, per_ty);
    per_ty[1] = '0';
    per_ty[11]='\0';

    switch (st.st_mode & S_IFMT) {
        case S_IFLNK:{
            char link_to[256];
            realpath(path, link_to);
            //int len = readlink(path, link_to, sizeof(link_to));
            //link_to[len] = '\0';
            struct stat link_file;
            stat(link_to, &link_file);
            per_ty[1] = get_file_type(link_file.st_mode);
            if (per_ty[1] == 'd') {
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%ld','n':'%s','p':'%s','lt':'%s','ct':'%ld'}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, link_to, get_file_count(path));
            } else {
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%ld','n':'%s','p':'%s','lt':'%s'}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, link_to);
            }
            break;
        }
        case S_IFDIR:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%ld','n':'%s','p':'%s','ct':'%ld'}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, get_file_count(path));
            break;
        default:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%ld','n':'%s','p':'%s'}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path);
            break;
    }
    //printf("\n");
}

/*
 * get user by uid
 */
static void user2uid(unsigned uid, char* out) {
    struct passwd *pw = getpwuid(uid);
    if (pw) {
        strcpy(out, pw->pw_name);
    } else {
        sprintf(out, "%d", uid);
    }
}

/*
 * get group of user by gid
 */
static void group2gid(unsigned gid, char* out) {
    struct group *gr = getgrgid(gid);
    if (gr) {
        strcpy(out, gr->gr_name);
    } else {
        sprintf(out, "%d", gid);
    }
}

/*
 * get permission of file or folder by mode
 */
static void get_file_permission(unsigned mode, char* per) {
    for (int i = 0; i < 9; i++) {
        if (mode & mode_p[i]) {
            per[i + 2] = rwx[i];
        } else {
            per[i + 2] = '-';
        }
    }
    if (mode & mode_p[9]) {
        if (per[4] != '-')
            per[4] = rwx[9];
        else per[4] = rwx[10];
    }
    if (mode & mode_p[10]) {
        if (per[7] != '-')
            per[7] = rwx[9];
        else per[7] = rwx[10];
    }
    if (mode & mode_p[11]) {
        if (per[10] != '-')
            per[10] = rwx[11];
        else per[10] = rwx[12];
    }
}

/*
 * get file type by mode
 */
static char get_file_type(unsigned mode) {
    //File Type
    switch (mode & S_IFMT) {
        case S_IFREG: return '-';//regular file
        case S_IFDIR: return 'd';//directory
        case S_IFLNK: return 'l';//symlink
        case S_IFBLK: return 'b';//block device
        case S_IFCHR: return 'c';//character device
        case S_IFIFO: return 'p';//FIFO/pipe
        case S_IFSOCK: return 's';//socket
        default:
            return '?';//unknown

    }
}

/*
 * get file numbers of in the dir
 */
static long get_file_count(char* dir) {
    DIR* d = opendir(dir);
    if (d == NULL) {
        return 0;
    }
    long count = 0;
    while (readdir(d) != 0) {
        count++;
    }
    closedir(d);
    return count - 2;
}

struct folders {
    char path[MAX_PATH];
    TAILQ_ENTRY(folders) tailq_entry;
};
static TAILQ_HEAD(,folders) head;

static void remove_tailq_item(struct folders* item) {
    TAILQ_REMOVE(&head, item, tailq_entry);
    free(item);
    item = NULL;
}

/**
 * get total size of file or dir
 */
static long long get_file_size(char* file) {
    TAILQ_INIT(&head);
    long long f_size = 0;

    char path2[MAX_PATH];
    if (strcmp(file, "/") == 0) {
        strcpy(path2, "");
    } else if (strcmp(file, ".") == 0 || file[0] != '/'){
        if (strcmp(file, ".") == 0) {
            sprintf(path2, "%s", getcwd(NULL, 0));
        } else {
            sprintf(path2, "%s/%s", getcwd(NULL, 0), file);
        }
        file = path2;
    }

    struct stat type;
    if (lstat(file, &type) == -1) {
        return f_size;
    }
    char t = get_file_type(type.st_mode);
    if (t == 'l') {
        char link_to[MAX_PATH];
        if (realpath(file, link_to) == NULL) {
            return f_size;
        }
        if (lstat(link_to, &type) == -1) {
            return f_size;
        }
        if (get_file_type(type.st_mode) != 'd') {
            return (long long)type.st_size;
        }
        //file = link_to;
    } else {
        if (t != 'd') {
            return (long long)type.st_size;
        }
    }

    struct folders *p1 = (struct folders*)malloc(sizeof(struct folders));
    strcpy(p1->path, file);
    TAILQ_INSERT_TAIL(&head, p1, tailq_entry);
    DIR* dr = NULL;
    char path[MAX_PATH];
    struct stat st;
    struct dirent *dp = NULL;
    while (!TAILQ_EMPTY(&head)) {
        dr = opendir(TAILQ_FIRST(&head)->path);
        if (dr == NULL) {
            remove_tailq_item(TAILQ_FIRST(&head));
            continue;
        }
        struct folders *fdir = NULL;
        while ((dp = readdir(dr)) != NULL) {
            if ((strcmp(dp->d_name, ".") == 0) || (strcmp(dp->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", TAILQ_FIRST(&head)->path, dp->d_name);
            if (lstat(path, &st) == -1) {
                continue;
            }
            if (get_file_type(st.st_mode) == 'd') {
                fdir = (struct folders*)malloc(sizeof(struct folders));
                strcpy(fdir->path, path);
                TAILQ_INSERT_TAIL(&head, fdir, tailq_entry);
                continue;
            }
            /*if (((long long)st.st_size) <= 0)
            printf("%s----->%lld\n",path, (long long)st.st_size);*/
            f_size += (long long)st.st_size;
        }
        remove_tailq_item(TAILQ_FIRST(&head));
    }

    //printf("{'totalSize':%lld}\n", f_size);
    return f_size;
}

//file copy, move, delete  start>>===================================================================================================================
static void delete_file(char *src);
static void copy_file(char *src_path, char *des_path);
static void move_file(char *src_path, char *des_path);

static void do_copy(char *src_path, char *des_path, long long total_len) {
    FILE *src,*des;
    des = fopen(des_path,"wb");
    if (des == NULL) {
        printf("{\"srcIsR\":true, \"desIsW\":false}\n");
        return;
    }
    src = fopen(src_path,"rb");

    char buff[1 << 20];
    size_t _size = sizeof(char);
    size_t size_n = sizeof(buff);

    int len;
    int count = 0;
    while(len = fread(buff, _size, size_n, src)) {
        fwrite(buff, 1, len, des);
        count += len;
        printf("{\"name\":\"%s\",\"totalSize\":%lld, \"currentSize\":%lld}\n",strrchr(src_path, '/') + 1, total_len, count);
    }
    fflush(des);
    fclose(src);
    fclose(des);
}

static void copy_file(char *src_path, char *des_path) {
    /*
     * 判断文件是否存在和是否可读可写
    int access(const char *pathname,int mode);
    pathname:是文件名称
    mode是我们要判断的属性.可以取以下值或者是他们的组合:
    R_OK文件可以读
    W_OK文件可以写
    X_OK文件可以执行
    F_OK文件存在.
    当我们测试成功时,函数返回0,否则如果有一个条件不符时,返回-1.
     * */
    if (access(src_path, R_OK)) {
        printf("{\"srcIsR\":false, \"desIsW\":true}\n");
        return;
    }
    if (access(des_path, W_OK)) {
        printf("{\"srcIsR\":true, \"desIsW\":false}\n");
        return;
    }
    struct stat st_src, st_des;
    if (stat(src_path, &st_src)) {
        printf("{\"srcIsR\":false, \"desIsW\":true}\n");
        return;
    }
    if (stat(des_path, &st_des)) {
        printf("{\"srcIsR\":true, \"desIsW\":false}\n");
        return;
    }

    char des_p[1024];
    sprintf(des_p, "%s%s", des_path, strrchr(src_path, '/'));
    if (S_ISDIR(st_src.st_mode)) {
        mkdir(des_p, 0777);
        char path[1024];
        struct dirent *dt;
        DIR *dir = opendir(src_path);
        while(dt = readdir(dir)) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src_path, dt->d_name);
            copy_file(path, des_p);
        }
        closedir(dir);
    } else {
        //printf("=============file1======%s\n", src_path);
        //printf("=============file2======%s\n", des_p);
        do_copy(src_path, des_p, (long long)st_src.st_size);
    }

}

static void move_file(char *src_path, char *des_path) {
    /*
     * 判断文件是否存在和是否可读可写
    int access(const char *pathname,int mode);
    pathname:是文件名称
    mode是我们要判断的属性.可以取以下值或者是他们的组合:
    R_OK文件可以读
    W_OK文件可以写
    X_OK文件可以执行
    F_OK文件存在.
    当我们测试成功时,函数返回0,否则如果有一个条件不符时,返回-1.
     * */
    if (access(src_path, R_OK)) {
        printf("{\"srcIsR\":false, \"desIsW\":true}\n");
        return;
    }
    if (access(des_path, W_OK)) {
        printf("{\"srcIsR\":true, \"desIsW\":false}\n");
        return;
    }
    struct stat st_src, st_des;
    if (stat(src_path, &st_src)) {
        printf("{\"srcIsR\":false, \"desIsW\":true}\n");
        return;
    }
    if (stat(des_path, &st_des)) {
        printf("{\"srcIsR\":true, \"desIsW\":false}\n");
        return;
    }

    char des_p[1024];
    sprintf(des_p, "%s%s", des_path, strrchr(src_path, '/'));
    if (S_ISDIR(st_src.st_mode)) {
        mkdir(des_p, 0777);
        char path[1024];
        struct dirent *dt;
        DIR *dir = opendir(src_path);
        while(dt = readdir(dir)) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src_path, dt->d_name);
            copy_file(path, des_p);
        }
        closedir(dir);
        delete_file(src_path);
    } else {
        //printf("=============file1======%s\n", src_path);
        //printf("=============file2======%s\n", des_p);
        do_copy(src_path, des_p, (long long)st_src.st_size);
        delete_file(src_path);
    }

}

static int file_not_rw(char *file) {
    return access(file, R_OK) | access(file, W_OK);
}

static void delete_file(char *src) {
    if (file_not_rw(src)) {
        printf("{\"state\":false, \"name\":\"%s\"}\n", strrchr(src, '/') + 1);
        return;
    }
    struct stat st;
    stat(src, &st);
    if (S_ISDIR(st.st_mode)) {
        char path[1024];
        struct dirent *dt;
        DIR *dir = opendir(src);
        while(dt = readdir(dir)) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src, dt->d_name);
            delete_file(path);
        }
        closedir(dir);
    }
    int state = remove(src);
    printf("{\"state\":%s, \"name\":\"%s\"}\n", state == 0 ? "true" : "false", strrchr(src, '/') + 1);
}
//file copy, move, delete  end <<====================================================================================================================

int main(int argc, char *argv[]) {

    char* file_name;
    if (argc > 1) {
        if (argc > 2) {
            file_name = argv[2];
        } else {
            file_name = ".";
        }
        if (strcmp(argv[1], "-s") == 0) {
            printf("{'totalSize':%lld}\n", get_file_size(file_name));
        } else if (strcmp(argv[1], "-f") == 0) {
            ls_file(file_name);
        }
    }

    //get_file_size("/home/lu/android_source/android-7.1.2_r1/out");
    //get_file_size("/home/lu/clion-work/hello/cmake-build-debug/mydir");
    //ls_file(file_name);
    //long long s = 35329641863;
    //printf("%lld   \n",s);


    return 0;
}