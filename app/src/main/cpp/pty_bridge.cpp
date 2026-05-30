#include <jni.h>
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <string>
#include <vector>

namespace {
constexpr const char* LOG_TAG = "crawlcipher_pty";

struct PtySession {
    pid_t pid;
    int masterFd;
};

void logError(const char* message) {
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s: %s", message, strerror(errno));
}

std::string toString(JNIEnv* env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char* raw = env->GetStringUTFChars(value, nullptr);
    std::string result(raw ? raw : "");
    if (raw) {
        env->ReleaseStringUTFChars(value, raw);
    }
    return result;
}

std::vector<std::string> toStringVector(JNIEnv* env, jobjectArray values) {
    std::vector<std::string> result;
    if (values == nullptr) {
        return result;
    }

    const jsize length = env->GetArrayLength(values);
    result.reserve(static_cast<size_t>(length));
    for (jsize i = 0; i < length; ++i) {
        auto item = reinterpret_cast<jstring>(env->GetObjectArrayElement(values, i));
        result.emplace_back(toString(env, item));
        env->DeleteLocalRef(item);
    }
    return result;
}

pid_t statusToExitCode(int status) {
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    if (WIFSIGNALED(status)) {
        return 128 + WTERMSIG(status);
    }
    return status;
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeStart(
    JNIEnv* env,
    jobject,
    jstring executablePath,
    jobjectArray args,
    jstring workingDirectory,
    jint rows,
    jint cols
) {
    std::string executable = toString(env, executablePath);
    if (executable.empty()) {
        return 0;
    }

    int masterFd = posix_openpt(O_RDWR | O_NOCTTY);
    if (masterFd < 0) {
        logError("posix_openpt failed");
        return 0;
    }

    if (grantpt(masterFd) != 0 || unlockpt(masterFd) != 0) {
        logError("grantpt/unlockpt failed");
        close(masterFd);
        return 0;
    }

    char* slaveName = ptsname(masterFd);
    if (slaveName == nullptr) {
        logError("ptsname failed");
        close(masterFd);
        return 0;
    }

    struct winsize size {};
    size.ws_row = static_cast<unsigned short>(rows > 0 ? rows : 24);
    size.ws_col = static_cast<unsigned short>(cols > 0 ? cols : 80);
    ioctl(masterFd, TIOCSWINSZ, &size);

    std::vector<std::string> arguments = toStringVector(env, args);
    std::string cwd = toString(env, workingDirectory);

    pid_t pid = fork();
    if (pid < 0) {
        logError("fork failed");
        close(masterFd);
        return 0;
    }

    if (pid == 0) {
        setsid();
        int slaveFd = open(slaveName, O_RDWR);
        if (slaveFd < 0) {
            _exit(127);
        }

        ioctl(slaveFd, TIOCSCTTY, 0);
        dup2(slaveFd, STDIN_FILENO);
        dup2(slaveFd, STDOUT_FILENO);
        dup2(slaveFd, STDERR_FILENO);
        if (slaveFd > STDERR_FILENO) {
            close(slaveFd);
        }
        close(masterFd);

        if (!cwd.empty()) {
            chdir(cwd.c_str());
        }

        std::vector<char*> argv;
        argv.reserve(arguments.size() + 2);
        argv.push_back(const_cast<char*>(executable.c_str()));
        for (auto& argument : arguments) {
            argv.push_back(const_cast<char*>(argument.c_str()));
        }
        argv.push_back(nullptr);

        execv(executable.c_str(), argv.data());
        _exit(127);
    }

    auto* session = new PtySession{pid, masterFd};
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeRead(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray buffer,
    jint offset,
    jint length
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr || buffer == nullptr || length <= 0) {
        return -1;
    }

    std::vector<jbyte> temp(static_cast<size_t>(length));
    ssize_t readCount = read(session->masterFd, temp.data(), static_cast<size_t>(length));
    if (readCount <= 0) {
        return static_cast<jint>(readCount);
    }

    env->SetByteArrayRegion(buffer, offset, static_cast<jsize>(readCount), temp.data());
    return static_cast<jint>(readCount);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeWrite(
    JNIEnv* env,
    jobject,
    jlong handle,
    jbyteArray data,
    jint offset,
    jint length
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr || data == nullptr || length <= 0) {
        return -1;
    }

    std::vector<jbyte> temp(static_cast<size_t>(length));
    env->GetByteArrayRegion(data, offset, length, temp.data());
    ssize_t wrote = write(session->masterFd, temp.data(), static_cast<size_t>(length));
    return static_cast<jint>(wrote);
}

extern "C" JNIEXPORT void JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeResize(
    JNIEnv*,
    jobject,
    jlong handle,
    jint rows,
    jint cols
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr) {
        return;
    }

    struct winsize size {};
    size.ws_row = static_cast<unsigned short>(rows > 0 ? rows : 24);
    size.ws_col = static_cast<unsigned short>(cols > 0 ? cols : 80);
    ioctl(session->masterFd, TIOCSWINSZ, &size);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeWait(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr) {
        return -1;
    }

    int status = 0;
    waitpid(session->pid, &status, 0);
    return statusToExitCode(status);
}

extern "C" JNIEXPORT void JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeStop(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr) {
        return;
    }
    kill(session->pid, SIGTERM);
}

extern "C" JNIEXPORT void JNICALL
Java_com_crawlcipher_wrapper_PtyBridge_nativeClose(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* session = reinterpret_cast<PtySession*>(handle);
    if (session == nullptr) {
        return;
    }
    close(session->masterFd);
    delete session;
}
