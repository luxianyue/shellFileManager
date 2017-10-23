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
    char str_time[17];
    time_t te = st.st_mtime;
    struct tm *t = localtime(&te);
    strftime(str_time, sizeof(str_time), "%Y-%m-%d %H:%M", t);

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
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','lt':'%s','ct':'%ld'}\n", per_ty, user, group, (long long)st.st_size, str_time, name, path, link_to, get_file_count(path));
            } else {
                printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','lt':'%s'}\n", per_ty, user, group, (long long)st.st_size, str_time, name, path, link_to);
            }
            break;
        }
        case S_IFDIR:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s','ct':'%ld'}\n", per_ty, user, group, (long long)st.st_size, str_time, name, path, get_file_count(path));
            break;
        default:
            printf("{'tp':'%s','u':'%s','g':'%s','s':%lld,'dt':'%s','n':'%s','p':'%s'}\n", per_ty, user, group, (long long)st.st_size, str_time, name, path);
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