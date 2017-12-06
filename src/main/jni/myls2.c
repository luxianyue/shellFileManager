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

#include <errno.h>

#define MAX_PATH 1024
const mode_t mode_p[] = {S_IRUSR, S_IWUSR, S_IXUSR, S_IRGRP, S_IWGRP, S_IXGRP, S_IROTH, S_IWOTH, S_IXOTH};
const char rwx[] = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x'};

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

    char user[16];
    char group[16];
    user2uid(st.st_uid, user);
    group2gid(st.st_gid, group);

    char str_time[17];
    time_t te = st.st_mtime;
    struct tm *t = localtime(&te);
    strftime(str_time, sizeof(str_time), "%Y-%m-%d %H:%M", t);

    char per[12];
    get_file_permission(st.st_mode, per);
    per[1] = '0';
    per[11]='\0';
    switch (st.st_mode & S_IFMT) {
        case S_IFLNK:{
            char link_to[256];
            realpath(path, link_to);
            //int len = readlink(path, link_to, sizeof(link_to));
            //link_to[len] = '\0';
            struct stat link_file;
            stat(link_to, &link_file);
            per[1] = get_file_type(link_file.st_mode);
            if (per[1] == 'd') {
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','lt':'%s','ct':%lld}\n", per, user, group, (long long)st.st_size, str_time, name, path, link_to, get_file_count(path));
            } else {
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','lt':'%s'}\n", per, user, group, (long long)st.st_size, str_time, name, path, link_to);
            }

            //printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','lt':'%s'}\n", per, user, group, (long long)st.st_size, str_time, name, path, link_to);
            break;
        }
        case S_IFDIR:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','ct':%ld}\n", per, user, group, (long long)st.st_size, str_time, name, path, get_file_count(path));
            break;
        default:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s'}\n", per, user, group, (long long)st.st_size, str_time, name, path);
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
    per[0] = get_file_type(mode);
    for (int i = 0; i < 9; i++) {
        if (mode & mode_p[i]) {
            per[i + 2] = rwx[i];
        } else {
            per[i + 2] = '-';
        }
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

int main(int argc, char *argv[]) {

    char *file_name;
    if (argc > 1) {
        file_name = argv[1];
    } else {
        file_name = ".";
    }

    ls_file(file_name);
    //printf("%ld\n", get_file_count(file_name));

    return 0;
}