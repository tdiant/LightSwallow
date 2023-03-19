#pragma once

class PosixPipe {
public:
    PosixPipe(int flags = 0);

    ~PosixPipe();

    int operator[](int);

private:
    int fd[2];
};
