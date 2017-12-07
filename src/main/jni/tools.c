//
// Created by bulefin on 2017/11/13.
//
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

#define MAX_PATH 1024
const mode_t mode_p[] = {S_IRUSR, S_IWUSR, S_IXUSR, S_IRGRP, S_IWGRP, S_IXGRP, S_IROTH, S_IWOTH, S_IXOTH, S_ISUID, S_ISGID, S_ISVTX};
const char rwx[] = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x', 's', 'S', 't', 'T'};

static void get_file_permission(unsigned mode, char* per);
static char get_file_type(unsigned mode);
static void group2gid(unsigned gid, char* out);
static void user2uid(unsigned uid, char* out);
static void ls_file_info(char *path, char* name);
static long get_file_count(char* dir);


static void insertChar(char *src ,char chr ,int nPos) {
    if (src == NULL || nPos > strlen(src)) {
        return;
    }

    char *p = src + strlen(src) + 1;  // p指向结束符的下一个位置
    while ((p - src) >= nPos){         // 没有=的话插入后的效果是插入字符位与第nPos+1的位置（从1开始数），这个自己根据情况选择
        *p = *(p-1);
        p--;
    }
    *p = chr;
}

static void check_char(char *path) {
    // fd\"fdf
    for (int i = 0; i < strlen(path); i++) {
        if (path[i] == '"' || path[i] == '\\') {
            insertChar(path, '\\', i + 1);
            i++;
        }
    }

}

/*
 * list file
 */
static void ls_file(char *file_path) {
    printf("ls_file------>%s\n", file_path);
    char temp[MAX_PATH];
    char path[MAX_PATH];
    //realpath(file_path, temp);
    struct dirent *dp;
    DIR *dir;
    if ((dir = opendir(file_path)) == NULL) {
        //perror("error");
        char *err = strerror(errno);
        char serror[strlen(err) + strlen(file_path) + 39];
        sprintf(serror, "{\"flag\":\"f\",\"path\":\"%s\",\"error\":\"%s\"}", file_path, err);
        fprintf(stderr, "%s\n", serror);
        //write(2, serror, strlen(serror));
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


    check_char(name);
    switch (st.st_mode & S_IFMT) {
        case S_IFLNK:{
            char link_to[256];
            realpath(path, link_to);
            //int len = readlink(path, link_to, sizeof(link_to));
            //link_to[len] = '\0';
            check_char(link_to);
            struct stat link_file;
            stat(link_to, &link_file);
            per_ty[1] = get_file_type(link_file.st_mode);
            if (per_ty[1] == 'd') {
                long count = get_file_count(path);
                check_char(path);
                printf("{\"flag\":\"f\",\"tp\":\"%s\",\"u\":\"%s\",\"g\":\"%s\",\"s\":\"%lld\",\"dt\":\"%ld\",\"n\":\"%s\",\"p\":\"%s\",\"lt\":\"%s\",\"ct\":\"%ld\"}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, link_to, count);
            } else {
                check_char(path);
                printf("{\"flag\":\"f\",\"tp\":\"%s\",\"u\":\"%s\",\"g\":\"%s\",\"s\":\"%lld\",\"dt\":\"%ld\",\"n\":\"%s\",\"p\":\"%s\",\"lt\":\"%s\"}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, link_to);
            }
            break;
        }
        case S_IFDIR:{
            long  cout = get_file_count(path);
            check_char(path);
            printf("{\"flag\":\"f\",\"tp\":\"%s\",\"u\":\"%s\",\"g\":\"%s\",\"s\":\"%lld\",\"dt\":\"%ld\",\"n\":\"%s\",\"p\":\"%s\",\"ct\":\"%ld\"}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path, cout);
            break;
        }
        default: {
            check_char(path);
            printf("{\"flag\":\"f\",\"tp\":\"%s\",\"u\":\"%s\",\"g\":\"%s\",\"s\":\"%lld\",\"dt\":\"%ld\",\"n\":\"%s\",\"p\":\"%s\"}\n", per_ty, user, group, (long long)st.st_size, st.st_mtime, name, path);
            break;
        }
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
    while (readdir(d) != NULL) {
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

    //printf("{'totalSize':'%lld'}\n", f_size);
    return f_size;
}

//file copy, move, delete  start>>===================================================================================================================
static void delete_file(char *flag, char *src);
static void copy_file(char *flag, char *src_path, char *des_path);
static void move_file(char *flag, char *src_path, char *des_path);
static void get_filesystem_dir(char *path, char r_path[]);

static void do_copy(char *flag, char *src_path, char *des_path, long long total_len) {
    FILE *src,*des;
    des = fopen(des_path,"wb");
    if (des == NULL) {
        printf("--des null->%s\n", strerror(errno));
        printf("{'srcIsR':'true', 'desIsW':'false'}\n");
        return;
    }
    src = fopen(src_path,"rb");

    char buff[1 << 16];
    size_t _size = sizeof(char);
    size_t size_n = sizeof(buff);

    int len = 0;
    long long count = 0;
    char f_name[strlen(strrchr(src_path, '/') + 1) + 60];
    strcpy(f_name, strrchr(src_path, '/') + 1);
    check_char(f_name);
    while((len = fread(buff, _size, size_n, src)) != 0) {
        fwrite(buff, _size, len, des);
        count += len;
        printf("{\"flag\":\"%s\",\"name\":\"%s\",\"totalSize\":\"%lld\", \"currentSize\":\"%lld\"}\n", flag, f_name, total_len, count);
    }
    if (count == 0) {
        printf("{\"flag\":\"%s\",\"name\":\"%s\",\"totalSize\":\"%lld\", \"currentSize\":\"%lld\"}\n", flag, f_name, total_len, count);
    }
    fflush(des);
    fclose(src);
    fclose(des);
}

static void copy_file(char *flag, char *src_path, char *des_path) {
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
   /* if (access(src_path, R_OK)) {
        printf("{'srcIsR':'false', 'desIsW':'true'}\n");
        return;
    }
    if (access(des_path, W_OK)) {
        printf("{'srcIsR':'true', 'desIsW':'false'}\n");
        return;
    }*/
    struct stat st_src, st_des;
    if (stat(src_path, &st_src)) {
        printf("--stat src->%s\n", strerror(errno));
        printf("{'srcIsR':'false', 'desIsW':'true'}\n");
        return;
    }
    if (stat(des_path, &st_des)) {
        printf("--stat des->%s\n", strerror(errno));
        printf("{'srcIsR':'true', 'desIsW':'false'}\n");
        return;
    }

    char des_p[MAX_PATH];
    sprintf(des_p, "%s%s", des_path, strrchr(src_path, '/'));
    if (S_ISDIR(st_src.st_mode)) {
        if (mkdir(des_p, 0777)){
            printf("--mkdir->%s\n", strerror(errno));
            return;
        }
        char f_name[strlen(strrchr(des_p, '/') + 1) + 60];
        strcpy(f_name, strrchr(des_p, '/') + 1);
        check_char(f_name);
        printf("{\"flag\":\"%s\",\"name\":\"%s\",\"totalSize\":\"%lld\", \"currentSize\":\"%lld\"}\n", flag, f_name, 0LL, 0LL);
        char path[MAX_PATH];
        struct dirent *dt;
        DIR *dir = opendir(src_path);
        if (dir == NULL) {
            printf("--dir null->%s\n", strerror(errno));
        }
        while((dt = readdir(dir)) != NULL) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src_path, dt->d_name);
            copy_file(flag, path, des_p);
        }
        closedir(dir);
    } else {
        //printf("=============file1======%s\n", src_path);
        //printf("=============file2======%s\n", des_p);
        do_copy(flag, src_path, des_p, (long long)st_src.st_size);
    }

}

static void move_file(char* flag, char *src_path, char *des_path) {
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
    /*if (access(src_path, R_OK)) {
        printf("{'srcIsR':'false', 'desIsW':'true'}\n");
        return;
    }
    if (access(des_path, W_OK)) {
        printf("{'srcIsR':'true', 'desIsW':'false'}\n");
        return;
    }*/
    struct stat st_src, st_des;
    if (stat(src_path, &st_src)) {
        printf("--stat src->%s\n", strerror(errno));
        printf("{'srcIsR':'false', 'desIsW':'true'}\n");
        return;
    }
    if (stat(des_path, &st_des)) {
        printf("--stat des->%s\n", strerror(errno));
        printf("{'srcIsR':'true', 'desIsW':'false'}\n");
        return;
    }

    char des_p[MAX_PATH];
    sprintf(des_p, "%s%s", des_path, strrchr(src_path, '/'));
    if (S_ISDIR(st_src.st_mode)) {
        if (mkdir(des_p, 0777)){
            printf("--mkdir->%s\n", strerror(errno));
        }
        char f_name[strlen(strrchr(des_p, '/') + 1) + 60];
        strcpy(f_name, strrchr(des_p, '/') + 1);
        check_char(f_name);
        printf("{\"flag\":\"%s\",\"name\":\"%s\",\"totalSize\":\"%lld\", \"currentSize\":\"%lld\"}\n", flag, f_name, 0LL, 0LL);
        char path[MAX_PATH];
        struct dirent *dt;
        DIR *dir = opendir(src_path);
        if (dir == NULL) {
            printf("--dir null->%s\n", strerror(errno));
        }
        while((dt = readdir(dir)) != NULL) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src_path, dt->d_name);
            copy_file(flag, path, des_p);
        }
        closedir(dir);
        delete_file(flag, src_path);
    } else {
        //printf("=============file1======%s\n", src_path);
        //printf("=============file2======%s\n", des_p);
        do_copy(flag, src_path, des_p, (long long)st_src.st_size);
        delete_file(flag, src_path);
    }

}

static int file_not_rw(char *file) {
    return access(file, R_OK) | access(file, W_OK);
}

static void delete_file(char *flag, char *src) {
    /*if (file_not_rw(src)) {
        printf("{'state':'false', 'name':'%s'}\n", strrchr(src, '/') + 1);
        return;
    }*/
    struct stat st;
    if (stat(src, &st)){
        printf("del stat--->%s\n", strerror(errno));
        return;
    }
    if (S_ISDIR(st.st_mode)) {
        char path[MAX_PATH];
        struct dirent *dt;
        DIR *dir = opendir(src);
        if (dir == NULL) {
            printf("del dir null--->%s\n", strerror(errno));
            return;
        }
        while((dt = readdir(dir)) != NULL) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src, dt->d_name);
            delete_file(flag, path);
        }
        closedir(dir);
    }
    int state = remove(src);
    if (state) {
        printf("del remove--->%s\n", strerror(errno));
    }
    if (strcmp(flag, "mv") != 0) {
        check_char(src);
        printf("{\"flag\":\"%s\",\"state\":\"%s\", \"name\":\"%s\"}\n", flag, state == 0 ? "true" : "false", strrchr(src, '/') + 1);
    }
}

static void get_filesystem_dir(char *path, char r_path[]) {
    int index = 0;
    for (int i = 0; i < strlen(path); i++) {
        if (path[i] == '/') {
            index++;
        }
        if (index > 1) {
            for (int k = 0; k < i; k++)
                r_path[k] = path[k];
            r_path[i] = '\0';
            break;
        }
    }
    if (index == 1) {
        r_path[0] = '/';
        r_path[1] = '\0';
    }
}
//file copy, move, delete  end <<====================================================================================================================

//create file or dir start >>=========================================================
#define FLAG_DIR 1
#define FLAG_FILE 2
static void create_file_or_dir(char *name, int flag) {
    if (access(name, F_OK) == 0) {
        printf("{\"flag\":\"nd\",\"state\":\"false\",\"reason\":\"File exists\",\"isOver\":\"true\"}\n");
        return;
    }
    if (flag == FLAG_DIR) {
        if (mkdir(name, 0777)){
            check_char(name);
            fprintf(stderr, "{\"flag\":\"nd\",\"error\":\"%s\",\"path\":\"%s\"}\n", strerror(errno), name);
        } else {
            printf("{\"flag\":\"nd\",\"state\":\"true\",\"isOver\":\"true\"}\n");
        }
        return;
    }
    if (flag == FLAG_FILE) {
        FILE *file = fopen(name, "w");
        if (file == NULL) {
            //printf("create fail--->%s\n", strerror(errno));
            check_char(name);
            fprintf(stderr, "{\"flag\":\"nf\",\"error\":\"%s\",\"path\":\"%s\"}\n", strerror(errno), name);
            return;
        }
        printf("{\"flag\":\"nf\",\"state\":\"true\",\"isOver\":\"true\"}\n");
        fclose(file);
    }
}
//create file or dir end <<===========================================================

// chmod start >>=====================================================================
static char* do_chmod(char *path, int mode, char *arg) {
    if (chmod(path, mode)) {
        char *error_msg = strerror(errno);
        char err[strlen(path) + strlen(error_msg) + 39];
        check_char(path);
        sprintf(err, "{\"flag\":\"chm\",\"error\":\"%s\",\"mode\":\"%s\",\"path\":\"%s\"}",error_msg, arg, path);
        fprintf(stderr, "%s\n", err);
        return strerror(errno);
    } else {
        check_char(path);
        char f_name[strlen(strrchr(path, '/') + 1) + 60];
        strcpy(f_name, strrchr(path, '/') + 1);
        printf("{\"flag\":\"chm\",\"state\":\"true\",\"mode\":\"%s\",\"path\":\"%s\", \"name\":\"%s\"}\n", arg, path, f_name);
        return NULL;
    }
}
static void chmod_file(char *src, int mode, char fg, char *arg, unsigned fg2) {
    if (fg2 == 1) {
        do_chmod(src, mode, arg);
        if (fg == 'o') {
            return;
        }
    }
    struct stat st;
    if (stat(src, &st)){
        printf("chmod stat--->%s\n", strerror(errno));
        return;
    }
    if (S_ISDIR(st.st_mode)) {
        if (fg2 != 1) {
            do_chmod(src, mode, arg);
        }
        char path[MAX_PATH];
        struct dirent *dt;
        DIR *dir = opendir(src);
        if (dir == NULL) {
            printf("chmod dir null--->%s\n", strerror(errno));
            return;
        }
        while((dt = readdir(dir)) != NULL) {
            if ((strcmp(dt->d_name, ".") == 0) || (strcmp(dt->d_name, "..") == 0)) {
                continue; //skip self and parent
            }
            sprintf(path, "%s/%s", src, dt->d_name);
            chmod_file(path, mode, fg, arg, 0);
        }
        closedir(dir);
    } else {
        if (fg == 'r' && fg2 != 1) {
            do_chmod(src, mode, arg);
        }
    }
}
// chmod end <<================================================================================

// text start >>=======================================================
static void do_text(char *src_path, char *des_path, char fg) {
    FILE *src,*des;
    if (fg == 'l') {
        des = fopen(des_path,"wb");
        src = fopen(src_path,"rb");
    }
    if (fg == 'e') {
        src = fopen(des_path,"rb");
        des = fopen(src_path,"wb");
    }
    if (src == NULL || des == NULL) {
        char *error_msg = strerror(errno);
        char err[strlen(src_path) + strlen(error_msg) + 39];
        check_char(src_path);
        sprintf(err, "{\"flag\":\"%ctext\",\"error\":\"%s\",\"path\":\"%s\"}", fg, error_msg, src_path);
        fprintf(stderr, "%s\n", err);
        if (fg == 'l') {
            fclose(des);
        }
        if (fg == 'e') {
            fclose(src);
        }
        return;
    }
    char buff[1 << 16];
    size_t _size = sizeof(char);
    size_t size_n = sizeof(buff);

    int len = 0;
    while((len = fread(buff, _size, size_n, src)) != 0) {
        fwrite(buff, _size, len, des);
    }
    fflush(des);
    fclose(src);
    fclose(des);
    check_char(src_path);
    printf("{\"flag\":\"%ctext\",\"isOver\":\"true\",\"path\":\"%s\"}\n", fg, src_path);
}
// text end << ========================================================

#include <pthread.h>

/* int len = 0;
    for (int i = 1; i < argc; i++) {
        len += strlen(argv[i]);
    }
    params *para = (params*)malloc(sizeof(params) + len);
    para->argc = argc;
    for (int i = 1; i < argc; i++) {
        para->index[i -1] = strlen(argv[i]);
        strcat(para->data, argv[i]);
        //printf("%d===%s\n", para->index[i-1], para->data);
    }
    //free(para);
    if (argc == 1) {
        return 0;
    }
    pthread_t phtid;
    if (pthread_create(&phtid, NULL, start_thread, (void *)para)){
        printf("pthread fail %s\n", strerror(errno));
    } else {
        printf("pthread succeed \n");
    }
    pthread_exit(0);*/

/*typedef struct {
    int argc;
    int index[3];
    char data[0];
}params;
static void* start_thread(void* param) {
    params *para = (params*)param;
    printf("argc---->%d\n", para->argc);
    printf("argv---->%s\n", para->data);
    char fg[para->index[0]];
    strncpy(fg, para->data, para->index[0]);
    fg[para->index[0]] = '\0';

    char* file_name;
    char name[para->index[1]];
    if (para->argc > 2) {
         strncpy(name, para->data + para->index[0], para->index[1]);
         name[para->index[1]] = '\0';
         file_name = name;
     } else {
         file_name = ".";
     }
     if (strcmp(fg, "-s") == 0) {
         printf("{'flag':'s','totalSize':'%lld'}\n", get_file_size(file_name));
         //return 0;
     } else if (strcmp(fg, "-f") == 0) {
         ls_file(file_name);
         _exit(0);
         //return 0;
     } else if (strcmp(fg, "-cp") == 0) {
         //copy file
         char des[para->index[2]];
         strncpy(des, para->data + para->index[0] + para->index[1], para->index[2]);
         des[para->index[2]] = '\0';
         copy_file("cp", file_name, des);
         //return 0;
     } else if (strcmp(fg, "-mv") == 0) {
         //move file
         char des[para->index[2]];
         strncpy(des, para->data + para->index[0] + para->index[1], para->index[2]);
         des[para->index[2]] = '\0';
         move_file("mv", file_name, des);
         //return 0;
     } else if (strcmp(fg, "-del") == 0) {
         //delete file
         delete_file("del", file_name);
         //return 0;
     } else if (strcmp(fg, "-rn") == 0) {
         //delete file
         char des[para->index[2]];
         strncpy(des, para->data + para->index[0] + para->index[1], para->index[2]);
         des[para->index[2]] = '\0';
         if (rename(file_name, des) == 0) {
             printf("{'flag':'rn','state':'true'}\n");
         } else {
             printf("{'flag':'rn','state':'false','reason':'%s'}\n", strerror(errno));
         }
         //return 0;
     } else if (strcmp(fg, "-nd") == 0) {
         //new dir
         create_file_or_dir(file_name, FLAG_DIR);
         //return 0;
     } else if (strcmp(fg, "-nf") == 0) {
         //new file
         create_file_or_dir(file_name, FLAG_FILE);
         //return 0;
     }
     free(para);
    return (void*)0;
}*/

#include <sys/mount.h>
void f(char *source, char *target, char *format, long flag){
    if (mount("/dev/block/system","/system","ext4", MS_REMOUNT, NULL)) {
        printf("mount error--->%s\n", strerror(errno));
    } else {
        printf("mount sucessful--->\n");
    }
}
void ff(char *arg) {
    printf("%s\n", arg);
}

int main(int argc, char *argv[]) {

    if (argc == 2) {
        ff(argv[1]);
        return 0;
    }

    char *fg = argv[1];
    int path_len = 1;
    if (argv[2] != NULL) {
        path_len = strlen(argv[2]) + 60;
    }
    char name[path_len];
    if (argc > 2) {
        strcpy(name, argv[2]);
    } else {
        strcpy(name, ".");
    }
    if (strcmp(fg, "-f") == 0) {
        ls_file(name);
        printf("{\"flag\":\"f\",\"isOver\":\"true\"}\n");
        return 0;
    }
    if (strcmp(fg, "-nd") == 0) {
        //new dir
        create_file_or_dir(name, FLAG_DIR);
        return 0;
    }
    if (strcmp(fg, "-nf") == 0) {
        //new file
        create_file_or_dir(name, FLAG_FILE);
        return 0;
    }
    if (strcmp(fg, "-rn") == 0) {
        //delete file
        char des[strlen(argv[3]) + 60];
        strcpy(des, argv[3]);
        if (rename(name, des) == 0) {
            check_char(des);
            printf("{\"flag\":\"rn\",\"isOver\":\"true\",\"state\":\"true\",\"path\":\"%s\"}\n", des);
        } else {
            printf("{\"flag\":\"rn\",\"isOver\":\"true\",\"state\":\"false\",\"reason\":\"%s\"}\n", strerror(errno));
        }
        return 0;
    }
    if (strcmp(fg, "-chm") == 0) {
        int mode = 0;
        const char* s = argv[3];
        while (*s) {
            if (*s >= '0' && *s <= '7') {
                mode = (mode << 3) | (*s - '0');
            } else {
                fprintf(stderr, "Bad mode\n");
                return 10;
            }
            s++;
        }
        //tools -chm -n 0777 /sdcard
        //fg= n-->self, d-->only dir, r-->all;
        char fg[2];
        sprintf(fg, "%s", argv[2]);
        for (int i =4; i < argc; i++) {
            char m_path[strlen(argv[i]) + 60];
            memset(m_path, 0, sizeof(m_path));
            memcpy(m_path, argv[i], strlen(argv[i]));
            chmod_file(m_path, mode, fg[1], argv[3], 1);
        }
        //check_char(m_path);
        printf("{\"flag\":\"chm\",\"isOver\":\"true\",\"state\":\"true\",\"mode\":\"%s\",\"path\":\"%s\"}\n", argv[3], "/sdcardabc");
        //if (result == NULL) {
        //} else {
        //    printf("{\"flag\":\"chm\",\"isOver\":\"true\",\"state\":\"false\",\"error\":\"%s\",\"mode\":\"%s\",\"path\":\"%s\"}\n", err, argv[3], m_path);
        //}
        return 0;
    }
    if (strcmp(fg, "-uid") == 0) {
        printf("--pid-->%d  ---uid--->%d  ---gid--->%d\n", getpid(), getuid(), getgid());
        printf("{\"flag\":\"uid\",\"isOver\":\"true\",\"uid\":\"%d\"}\n", getuid());
        return 0;
    }
    if (strcmp(fg, "-ltext") == 0) {
        do_text(name, argv[3], 'l');
        return 0;
    }
    if (strcmp(fg, "-etext") == 0) {
        do_text(name, argv[3], 'e');
        return 0;
    }
    signal(SIGCHLD, SIG_IGN);
    if (fork() == 0) {
        if (strcmp(fg, "-s") == 0) {
            printf("{\"flag\":\"s\",\"isOver\":\"true\",\"totalSize\":\"%lld\"}\n", get_file_size(name));
            return 0;
        }else if (strcmp(fg, "-cp") == 0) {
            //copy file

            for (int i = 3; i < argc; i++) {
                char des[strlen(argv[i]) + 60];
                memset(des, 0, sizeof(des));
                memcpy(des, argv[i], strlen(argv[i]));
                copy_file("cp", des, name);
            }
            printf("{\"flag\":\"cp\",\"isOver\":\"true\"}\n");
            return 0;
        } else if (strcmp(fg, "-mv") == 0) {
            //move file
            for (int i = 3; i < argc; i++) {
                char des[strlen(argv[i]) + 60];
                memset(des, 0, sizeof(des));
                memcpy(des, argv[i], strlen(argv[i]));
                move_file("mv", des, name);
            }
            printf("{\"flag\":\"mv\",\"isOver\":\"true\"}\n");
            return 0;
        } else if (strcmp(fg, "-del") == 0) {
            //delete file
            for (int i = 2; i < argc; i++) {
                char des[strlen(argv[i]) + 60];
                memset(des, 0, sizeof(des));
                memcpy(des, argv[i], strlen(argv[i]));
                delete_file("del", des);
            }
            printf("{\"flag\":\"del\",\"isOver\":\"true\"}\n");
            return 0;
        }

    } else {
        return 0;
    }
    return 0;
}
